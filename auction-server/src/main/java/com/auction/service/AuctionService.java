package com.auction.service;

import com.auction.dao.AuctionDAO;
import com.auction.dao.BidTransactionDAO;
import com.auction.dao.IAuctionDAO;
import com.auction.dao.IBidTransactionDAO;
import com.auction.dao.IUserDAO;
import com.auction.dao.UserDAO;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuctionNotFoundException;
import com.auction.exception.DataValidationException;
import com.auction.exception.InvalidBidException;
import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.model.Status;
import com.auction.model.User;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * - Quản lý toàn bộ nghiệp vụ đấu giá, xử lý khóa đa luồng (ReentrantLock)
 *   chống Race Condition và Lost Update khi nhiều Client cùng bid đồng thời.
 * - Tự động kích hoạt thuật toán Anti-sniping (+60s nếu có bid ở 30s cuối).
 * - Tách biệt độc lập khỏi luồng mạng ClientHandler để dễ viết Unit Test JUnit 5.
 */
public class AuctionService {

    private static AuctionService instance;

    /** Ngưỡng thời gian cuối phiên kích hoạt cơ chế gia hạn Anti-sniping (30 giây). */
    public static final long ANTI_SNIPING_THRESHOLD_SECONDS = 30;

    /** Thời gian tự động cộng thêm khi có lượt đặt giá ở phút chót (60 giây). */
    public static final long ANTI_SNIPING_EXTENSION_SECONDS = 60;

    /**
     * Bản đồ khóa độc lập cho từng phiên đấu giá (Fine-grained Lock bằng ReentrantLock).
     * Giúp các Client đặt giá ở các phiên khác nhau không bị chặn lẫn nhau.
     */
    private final Map<Integer, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    private final IAuctionDAO auctionDAO;
    private final IBidTransactionDAO bidDAO;
    private final IUserDAO userDAO;

    /**
     * Hàm dựng mặc định dùng DAO thực tế.
     */
    public AuctionService() {
        this(new AuctionDAO(), new BidTransactionDAO(), new UserDAO());
    }

    /**
     * Hàm dựng Dependency Injection (hỗ trợ Mock test trong JUnit).
     */
    public AuctionService(IAuctionDAO auctionDAO, IBidTransactionDAO bidDAO, IUserDAO userDAO) {
        this.auctionDAO = auctionDAO;
        this.bidDAO = bidDAO;
        this.userDAO = userDAO;
    }

    /**
     * Singleton Pattern lấy thể hiện dùng chung của AuctionService.
     */
    public static synchronized AuctionService getInstance() {
        if (instance == null) {
            instance = new AuctionService();
        }
        return instance;
    }

    /**
     * Xử lý nghiệp vụ đặt giá (placeBid) an toàn đa luồng.
     *
     * @param auctionId Mã phiên đấu giá
     * @param bidderId  Mã người đặt giá
     * @param amount    Số tiền đặt
     * @return Phiên đấu giá sau khi đã cập nhật giá mới và thời gian gia hạn
     */
    public Auction placeBid(int auctionId, int bidderId, double amount) {
        if (auctionId <= 0 || bidderId <= 0 || amount <= 0) {
            throw new DataValidationException("Thông tin đặt giá (auctionId, bidderId, amount) không hợp lệ.");
        }

        // Lấy hoặc tạo mới ReentrantLock riêng cho phiên đấu giá này
        ReentrantLock lock = auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock());
        lock.lock();
        try {
            // 1. Kiểm tra sự tồn tại của phiên và người đấu giá
            Auction auction = auctionDAO.findById(auctionId);
            if (auction == null) {
                throw new AuctionNotFoundException("Không tìm thấy phiên đấu giá #" + auctionId);
            }

            User bidder = userDAO.findById(bidderId);
            if (bidder == null) {
                throw new DataValidationException("Không tìm thấy người đặt giá #" + bidderId);
            }

            // 2. Nạp danh sách bid hiện tại và cập nhật trạng thái phiên
            List<BidTransaction> existingBids = bidDAO.findByAuctionId(auctionId);
            auction.setBids(existingBids);
            if (auction.getStatus() == Status.OPEN) {
                auction.setStatus(Status.RUNNING);
            }

            // 3. Thực hiện đặt giá (nếu giá thấp hơn hiện tại sẽ văng InvalidBidException)
            auction.placeBid(bidder, amount);

            // 4. Thuật toán Anti-sniping: Nếu bid trong 30s cuối -> gia hạn thêm 60s
            LocalDateTime now = LocalDateTime.now();
            if (auction.getEndTime() != null && auction.getEndTime().isAfter(now)) {
                long remainingSeconds = Duration.between(now, auction.getEndTime()).getSeconds();
                if (remainingSeconds <= ANTI_SNIPING_THRESHOLD_SECONDS) {
                    auction.setEndTime(auction.getEndTime().plusSeconds(ANTI_SNIPING_EXTENSION_SECONDS));
                }
            }

            // 5. Lưu xuống CSDL qua DAO
            auctionDAO.update(auction);
            BidTransaction newTx = new BidTransaction(null, auctionId, amount, bidder, now);
            bidDAO.save(newTx);

            return auction;
        } finally {
            lock.unlock(); // Luôn giải phóng khóa an toàn
        }
    }

    public Auction getAuctionById(int auctionId) {
        return auctionDAO.findById(auctionId);
    }

    public List<Auction> getAllAuctions() {
        return auctionDAO.findAll();
    }
}