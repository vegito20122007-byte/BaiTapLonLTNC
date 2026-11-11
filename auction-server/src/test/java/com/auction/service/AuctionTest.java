package com.auction.service;

import com.auction.dao.IAuctionDAO;
import com.auction.dao.IBidTransactionDAO;
import com.auction.dao.IUserDAO;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuctionNotFoundException;
import com.auction.exception.DataValidationException;
import com.auction.exception.InvalidBidException;
import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.model.Bidder;
import com.auction.model.Electronics;
import com.auction.model.Seller;
import com.auction.model.Status;
import com.auction.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử toàn diện:
 * 1. Bắt lỗi tham số không hợp lệ (DataValidationException)
 * 2. Bắt lỗi không tìm thấy phiên hoặc người dùng (AuctionNotFoundException)
 * 3. Đặt giá thành công hợp lệ
 * 4. Đặt giá thấp hơn mức giá hiện tại (InvalidBidException)
 * 5. Người bán tự đặt giá sản phẩm của mình (InvalidBidException)
 * 6. Đặt giá khi phiên đã kết thúc (AuctionClosedException)
 * 7. Thuật toán Anti-sniping (+60s khi bid ở 30s cuối)
 * 8. KIỂM THỬ RACE CONDITION: 10 luồng đặt giá đồng thời chống Lost Update
 */
public class AuctionTest {

    private AuctionService auctionService;
    private Map<Integer, Auction> fakeAuctionDb;
    private Map<Integer, User> fakeUserDb;
    private List<BidTransaction> fakeBidDb;
    private Seller seller;

    @BeforeEach
    void setUp() {
        fakeAuctionDb = new ConcurrentHashMap<>();
        fakeUserDb = new ConcurrentHashMap<>();
        fakeBidDb = Collections.synchronizedList(new ArrayList<>());

        // 1. Tạo Người bán (Seller) và Người đấu giá (Bidder)
        seller = new Seller(100, "seller_pro", "123456");
        fakeUserDb.put(100, seller);

        for (int i = 1; i <= 20; i++) {
            fakeUserDb.put(i, new Bidder(i, "bidder_" + i, "123456"));
        }

        // 2. Tạo món đồ Electronics và phiên đấu giá mẫu (Giá khởi điểm 10 triệu)
        Electronics laptop = new Electronics(1, "MacBook Pro M3", "Mới 100%", seller, "Apple", 12);
        Auction auction = new Auction(
                1,
                "Đấu giá MacBook Pro",
                laptop,
                seller,
                10000000.0, // startingPrice: 10 triệu
                15000000.0, // reservePrice: 15 triệu
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(10) // còn 10 phút nữa kết thúc
        );
        auction.setStatus(Status.RUNNING);
        fakeAuctionDb.put(1, auction);

        // 3. Khởi tạo Mock DAO chuẩn theo đúng kiểu trả về void của Interface
        IAuctionDAO mockAuctionDAO = new IAuctionDAO() {
            @Override public Auction findById(Integer id) { return fakeAuctionDb.get(id); }
            @Override public List<Auction> findAll() { return new ArrayList<>(fakeAuctionDb.values()); }
            @Override public void save(Auction a) { fakeAuctionDb.put(a.getId(), a); }
            @Override public void update(Auction a) { fakeAuctionDb.put(a.getId(), a); }
            @Override public void delete(Integer id) { fakeAuctionDb.remove(id); }
        };

        IBidTransactionDAO mockBidDAO = new IBidTransactionDAO() {
            @Override public void save(BidTransaction tx) { fakeBidDb.add(tx); }
            @Override public List<BidTransaction> findByAuctionId(Integer auctionId) {
                return fakeBidDb.stream().filter(b -> b.getAuctionId().equals(auctionId)).toList();
            }
            @Override public List<BidTransaction> findAll() { return new ArrayList<>(fakeBidDb); }
        };

        IUserDAO mockUserDAO = new IUserDAO() {
            @Override public User findById(Integer id) { return fakeUserDb.get(id); }
            @Override public User findByUsername(String username) {
                return fakeUserDb.values().stream()
                        .filter(u -> u.getUsername().equals(username))
                        .findFirst().orElse(null);
            }
            @Override public List<User> findAll() { return new ArrayList<>(fakeUserDb.values()); }
            @Override public void save(User u) { fakeUserDb.put(u.getId(), u); }
            @Override public void update(User u) { fakeUserDb.put(u.getId(), u); }
            @Override public void delete(Integer id) { fakeUserDb.remove(id); }
        };

        // Tiêm Mock DAO vào AuctionService
        auctionService = new AuctionService(mockAuctionDAO, mockBidDAO, mockUserDAO);
    }

    @Test
    @DisplayName("1. Lỗi tham số không hợp lệ (số tiền <= 0)")
    void testInvalidAmountThrowsException() {
        assertThrows(DataValidationException.class, () -> auctionService.placeBid(1, 1, -50000.0));
        assertThrows(DataValidationException.class, () -> auctionService.placeBid(1, 1, 0.0));
    }

    @Test
    @DisplayName("2. Lỗi không tìm thấy phiên đấu giá (auctionId không tồn tại)")
    void testAuctionNotFoundThrowsException() {
        assertThrows(AuctionNotFoundException.class, () -> auctionService.placeBid(9999, 1, 15000000.0));
    }

    @Test
    @DisplayName("3. Lỗi không tìm thấy người đặt giá (bidderId không tồn tại)")
    void testBidderNotFoundThrowsException() {
        assertThrows(DataValidationException.class, () -> auctionService.placeBid(1, 8888, 15000000.0));
    }

    @Test
    @DisplayName("4. Đặt giá thành công hợp lệ")
    void testPlaceBidSuccess() {
        Auction updated = auctionService.placeBid(1, 1, 12000000.0);

        assertEquals(12000000.0, updated.getCurrentPrice(), "Giá hiện tại phải là 12 triệu");
        assertEquals("bidder_1", updated.getCurrentWinner().getUsername(), "Người thắng hiện tại là bidder_1");
        assertEquals(1, fakeBidDb.size(), "Phải lưu đúng 1 giao dịch vào CSDL");
    }

    @Test
    @DisplayName("5. Đặt giá thấp hơn mức giá cao nhất hiện tại sẽ bị từ chối")
    void testPlaceBidLowerThanCurrentPrice() {
        auctionService.placeBid(1, 1, 12000000.0);

        // Người thứ 2 cố tình bid 11 triệu (< 12 triệu)
        assertThrows(InvalidBidException.class, () -> auctionService.placeBid(1, 2, 11000000.0));
    }

    @Test
    @DisplayName("6. Người bán không thể tự đặt giá cho sản phẩm của mình")
    void testSellerCannotBidOwnItem() {
        // ID 100 là seller của phiên này
        assertThrows(InvalidBidException.class, () -> auctionService.placeBid(1, 100, 20000000.0));
    }

    @Test
    @DisplayName("7. Đặt giá khi phiên đã kết thúc (FINISHED) sẽ bị từ chối")
    void testPlaceBidWhenAuctionClosed() {
        Auction auction = fakeAuctionDb.get(1);
        auction.setStatus(Status.FINISHED);

        assertThrows(AuctionClosedException.class, () -> auctionService.placeBid(1, 1, 15000000.0));
    }

    @Test
    @DisplayName("8. Thuật toán Anti-sniping: Gia hạn thêm 60 giây khi bid ở 30 giây cuối")
    void testAntiSnipingExtension() {
        Auction auction = fakeAuctionDb.get(1);
        LocalDateTime originalEnd = LocalDateTime.now().plusSeconds(15); // Chỉ còn 15 giây (<= 30s)
        auction.setEndTime(originalEnd);

        auctionService.placeBid(1, 1, 12000000.0);

        Auction updated = fakeAuctionDb.get(1);
        assertTrue(updated.getEndTime().isAfter(originalEnd), "Thời gian kết thúc phải được gia hạn");
        assertEquals(originalEnd.plusSeconds(60).getSecond(), updated.getEndTime().getSecond(), "Phải cộng thêm đúng 60 giây");
    }

    @Test
    @DisplayName("9. KIỂM THỬ RACE CONDITION: 10 Luồng đặt giá đồng thời chống Lost Update")
    void testConcurrentBiddingRaceCondition() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        // CountDownLatch giúp 10 luồng xuất phát tại cùng một tích tắc
        CountDownLatch readyGate = new CountDownLatch(numberOfThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (int i = 1; i <= numberOfThreads; i++) {
            final int bidderId = i;
            // Mỗi người đặt một mức giá tăng dần: 11tr, 12tr, ..., 20tr
            final double bidAmount = 10000000.0 + (i * 1000000.0);

            executor.submit(() -> {
                readyGate.countDown();
                try {
                    startGate.await(); // Chờ phát súng xuất phát
                    auctionService.placeBid(1, bidderId, bidAmount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Nếu bid đến sau nhưng giá lại thấp hơn mức giá cao nhất tại thời điểm đó -> Bị loại hợp lệ
                    rejectedCount.incrementAndGet();
                } finally {
                    endGate.countDown();
                }
            });
        }

        readyGate.await(); // Đảm bảo 10 luồng đã sẵn sàng ở vạch xuất phát
        startGate.countDown(); // BẮN SÚNG: 10 luồng đồng loạt gọi placeBid()

        boolean completed = endGate.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Hệ thống không được xảy ra Deadlock trong 5 giây!");

        Auction finalAuction = fakeAuctionDb.get(1);

        System.out.println("====== KẾT QUẢ TEST RACE CONDITION ======");
        System.out.println("Số lượt bid thành công: " + successCount.get());
        System.out.println("Số lượt bid bị từ chối hợp lệ: " + rejectedCount.get());
        System.out.println("Giá chốt phiên cao nhất: " + String.format("%,.0f VNĐ", finalAuction.getCurrentPrice()));
        System.out.println("Người dẫn đầu: " + finalAuction.getCurrentWinner().getUsername());
        System.out.println("=========================================");

        // Kiểm tra tính toàn vẹn dữ liệu
        assertTrue(successCount.get() >= 1, "Phải có ít nhất 1 bid thành công");
        assertEquals(numberOfThreads, successCount.get() + rejectedCount.get(), "Tổng số request xử lý phải đúng bằng 10");
        assertTrue(finalAuction.getCurrentPrice() >= 11000000.0, "Giá cuối cùng không bị mất mát (Chống Lost Update)");
        assertEquals(successCount.get(), fakeBidDb.size(), "Số giao dịch lưu CSDL phải bằng số lượt bid thành công");
    }
}