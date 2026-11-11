package com.auction.exception;

import com.auction.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bộ kiểm thử đơn vị JUnit 5 cho các Custom Exceptions theo lộ trình Tuần 8.
 */
@DisplayName("Kiểm thử Custom Exceptions (Tuần 8)")
public class AuctionExceptionTest {

    @Nested
    @DisplayName("1. Kiểm thử lớp gốc AuctionException")
    class AuctionExceptionBaseTest {

        @Test
        @DisplayName("AuctionException phải là một RuntimeException (Unchecked Exception)")
        void testAuctionExceptionInheritance() {
            AuctionException ex = new AuctionException("Lỗi hệ thống đấu giá");
            assertInstanceOf(RuntimeException.class, ex);
            assertEquals("Lỗi hệ thống đấu giá", ex.getMessage());
        }

        @Test
        @DisplayName("Khởi tạo AuctionException với message và cause")
        void testAuctionExceptionWithCause() {
            Throwable cause = new IllegalArgumentException("Nguyên nhân gốc");
            AuctionException ex = new AuctionException("Thông báo lỗi", cause);

            assertEquals("Thông báo lỗi", ex.getMessage());
            assertSame(cause, ex.getCause());
        }
    }

    @Nested
    @DisplayName("2. Kiểm thử các lớp ngoại lệ con kế thừa AuctionException")
    class SubExceptionsTest {

        @Test
        @DisplayName("InvalidBidException kế thừa từ AuctionException")
        void testInvalidBidException() {
            InvalidBidException ex = new InvalidBidException("Giá đặt thấp hơn giá hiện tại");
            assertInstanceOf(AuctionException.class, ex);
            assertEquals("Giá đặt thấp hơn giá hiện tại", ex.getMessage());

            Throwable cause = new RuntimeException("Root error");
            InvalidBidException exWithCause = new InvalidBidException("Lỗi đặt giá", cause);
            assertSame(cause, exWithCause.getCause());
        }

        @Test
        @DisplayName("AuctionClosedException kế thừa từ AuctionException")
        void testAuctionClosedException() {
            AuctionClosedException ex = new AuctionClosedException("Phiên đấu giá đã kết thúc");
            assertInstanceOf(AuctionException.class, ex);
            assertEquals("Phiên đấu giá đã kết thúc", ex.getMessage());

            Throwable cause = new IllegalStateException("Closed");
            AuctionClosedException exWithCause = new AuctionClosedException("Phiên đóng", cause);
            assertSame(cause, exWithCause.getCause());
        }

        @Test
        @DisplayName("AuthenticationException kế thừa từ AuctionException")
        void testAuthenticationException() {
            AuthenticationException ex = new AuthenticationException("Sai tên đăng nhập hoặc mật khẩu");
            assertInstanceOf(AuctionException.class, ex);
            assertEquals("Sai tên đăng nhập hoặc mật khẩu", ex.getMessage());
        }

        @Test
        @DisplayName("InsufficientBalanceException kế thừa từ AuctionException")
        void testInsufficientBalanceException() {
            InsufficientBalanceException ex = new InsufficientBalanceException("Số dư tài khoản không đủ để đặt giá");
            assertInstanceOf(AuctionException.class, ex);
            assertEquals("Số dư tài khoản không đủ để đặt giá", ex.getMessage());
        }

        @Test
        @DisplayName("DataValidationException kế thừa từ AuctionException")
        void testDataValidationException() {
            DataValidationException ex = new DataValidationException("Tên sản phẩm không được để trống");
            assertInstanceOf(AuctionException.class, ex);
            assertEquals("Tên sản phẩm không được để trống", ex.getMessage());
        }

        @Test
        @DisplayName("AuctionNotFoundException kế thừa từ AuctionException")
        void testAuctionNotFoundException() {
            AuctionNotFoundException ex = new AuctionNotFoundException("Không tìm thấy phiên đấu giá #99");
            assertInstanceOf(AuctionException.class, ex);
            assertEquals("Không tìm thấy phiên đấu giá #99", ex.getMessage());
        }

        @Test
        @DisplayName("ConnectionException kế thừa từ AuctionException")
        void testConnectionException() {
            ConnectionException ex = new ConnectionException("Mất kết nối với Socket Server");
            assertInstanceOf(AuctionException.class, ex);
            assertEquals("Mất kết nối với Socket Server", ex.getMessage());
        }

        @Test
        @DisplayName("ItemOwnershipException kế thừa từ AuctionException")
        void testItemOwnershipException() {
            ItemOwnershipException ex = new ItemOwnershipException("Sản phẩm không thuộc quyền sở hữu của bạn");
            assertInstanceOf(AuctionException.class, ex);
            assertEquals("Sản phẩm không thuộc quyền sở hữu của bạn", ex.getMessage());

            Throwable cause = new IllegalAccessException("Truy cập trái phép vật phẩm");
            ItemOwnershipException exWithCause = new ItemOwnershipException("Lỗi quyền sở hữu", cause);
            assertSame(cause, exWithCause.getCause());
        }

        @Test
        @DisplayName("UnauthorizedActionException kế thừa từ AuctionException")
        void testUnauthorizedActionException() {
            UnauthorizedActionException ex = new UnauthorizedActionException("Bạn không có quyền thực hiện hành động này");
            assertInstanceOf(AuctionException.class, ex);
            assertEquals("Bạn không có quyền thực hiện hành động này", ex.getMessage());

            Throwable cause = new SecurityException("Không đủ quyền hạn");
            UnauthorizedActionException exWithCause = new UnauthorizedActionException("Hành động bị từ chối", cause);
            assertSame(cause, exWithCause.getCause());
        }

        @Test
        @DisplayName("DatabaseOperationException kế thừa từ AuctionException")
        void testDatabaseOperationException() {
            DatabaseOperationException ex = new DatabaseOperationException("Lỗi truy vấn cơ sở dữ liệu");
            assertInstanceOf(AuctionException.class, ex);
            assertEquals("Lỗi truy vấn cơ sở dữ liệu", ex.getMessage());

            Throwable cause = new RuntimeException("SQL syntax error");
            DatabaseOperationException exWithCause = new DatabaseOperationException("Lỗi thao tác DB", cause);
            assertSame(cause, exWithCause.getCause());

            DatabaseOperationException exWithCauseOnly = new DatabaseOperationException(cause);
            assertSame(cause, exWithCauseOnly.getCause());
            assertEquals("SQL syntax error", exWithCauseOnly.getMessage());
        }
    }

    @Nested
    @DisplayName("3. Kiểm thử tích hợp: Logic đấu giá (placeBid & kết thúc phiên)")
    class AuctionIntegrationTest {

        private Auction auction;
        private Seller seller;
        private User bidder1;
        private User bidder2;

        @BeforeEach
        void setUp() {
            seller = new Seller(1, "seller_user", "pass123");
            bidder1 = new Bidder(2, "bidder_01", "pass123");
            bidder2 = new Bidder(3, "bidder_02", "pass123");

            Item item = new Electronics(10, "Laptop Dell XPS", "i7 16GB", seller, "Dell", 12);

            LocalDateTime now = LocalDateTime.now();
            auction = new Auction(
                    1,
                    "Đấu giá Laptop Dell XPS",
                    item,
                    seller,
                    15000000.0, // Khởi điểm: 15tr
                    18000000.0, // Giá sàn: 18tr
                    now.minusHours(1),
                    now.plusHours(2)
            );
            auction.updateStatus();
        }

        @Test
        @DisplayName("Tung AuctionClosedException khi phiên đấu giá không ở trạng thái RUNNING")
        void testPlaceBidWhenClosed() {
            auction.setStatus(Status.FINISHED);

            assertThrows(AuctionClosedException.class, () -> {
                auction.placeBid(bidder1, 20000000.0);
            }, "Phải ném AuctionClosedException khi phiên đã đóng");
        }

        @Test
        @DisplayName("Tung InvalidBidException khi thông tin người đặt giá là null")
        void testPlaceBidWithNullBidder() {
            assertThrows(InvalidBidException.class, () -> {
                auction.placeBid(null, 20000000.0);
            });

            User noIdBidder = new Bidder(null, "temp", "pass");
            assertThrows(InvalidBidException.class, () -> {
                auction.placeBid(noIdBidder, 20000000.0);
            });
        }

        @Test
        @DisplayName("Tung InvalidBidException khi người bán tự đặt giá sản phẩm của chính mình")
        void testPlaceBidSellerSelfBidding() {
            assertThrows(InvalidBidException.class, () -> {
                auction.placeBid(seller, 20000000.0);
            }, "Người bán không được tự đặt giá sản phẩm của mình");
        }

        @Test
        @DisplayName("Tung InvalidBidException khi số tiền đặt giá <= 0")
        void testPlaceBidInvalidAmount() {
            assertThrows(InvalidBidException.class, () -> {
                auction.placeBid(bidder1, 0.0);
            });

            assertThrows(InvalidBidException.class, () -> {
                auction.placeBid(bidder1, -50000.0);
            });
        }

        @Test
        @DisplayName("Tung InvalidBidException khi bid đầu tiên thấp hơn giá khởi điểm")
        void testPlaceBidBelowStartingPrice() {
            assertThrows(InvalidBidException.class, () -> {
                auction.placeBid(bidder1, 14000000.0); // Khởi điểm là 15tr
            });
        }

        @Test
        @DisplayName("Tung InvalidBidException khi bid thấp hơn hoặc bằng giá cao nhất hiện tại")
        void testPlaceBidBelowCurrentPrice() {
            auction.placeBid(bidder1, 16000000.0);
            assertEquals(16000000.0, auction.getMaxBid());

            // Đặt thấp hơn 16tr
            assertThrows(InvalidBidException.class, () -> {
                auction.placeBid(bidder2, 15500000.0);
            });

            // Đặt bằng đúng 16tr
            assertThrows(InvalidBidException.class, () -> {
                auction.placeBid(bidder2, 16000000.0);
            });
        }

        @Test
        @DisplayName("Đặt giá hợp lệ thành công và cập nhật đúng thông tin")
        void testPlaceBidSuccess() {
            assertDoesNotThrow(() -> auction.placeBid(bidder1, 16000000.0));
            assertEquals(16000000.0, auction.getMaxBid());
            assertEquals(bidder1, auction.getCurrentWinner());

            assertDoesNotThrow(() -> auction.placeBid(bidder2, 17000000.0));
            assertEquals(17000000.0, auction.getMaxBid());
            assertEquals(bidder2, auction.getCurrentWinner());
            assertEquals(2, auction.getBids().size());
        }

        @Test
        @DisplayName("Kết thúc phiên thành công khi mức giá chốt đạt hoặc vượt giá sàn (reservePrice)")
        void testAuctionEndWithReservePriceMet() {
            // Đặt giá 19tr (vượt mức giá sàn reservePrice = 18tr)
            auction.placeBid(bidder1, 19000000.0);
            assertEquals(19000000.0, auction.getMaxBid());
            assertEquals(bidder1, auction.getCurrentWinner());

            // Kết thúc phiên
            auction.finalProcess();

            assertEquals(Status.FINISHED, auction.getStatus());
            assertEquals(bidder1, auction.getCurrentWinner());
        }

        @Test
        @DisplayName("Hủy phiên khi kết thúc nếu mức giá chốt chưa đạt giá sàn (reservePrice)")
        void testAuctionEndWithReservePriceNotMet() {
            // Đặt giá 16tr (thấp hơn mức giá sàn 18tr)
            auction.placeBid(bidder1, 16000000.0);
            assertEquals(16000000.0, auction.getMaxBid());

            // Kết thúc phiên
            auction.finalProcess();

            // Phiên bị hủy vì chưa đạt giá sàn
            assertEquals(Status.CANCELED, auction.getStatus());
            assertNull(auction.getCurrentWinner());
        }

        @Test
        @DisplayName("Tự động kết thúc phiên khi thời gian hiện tại vượt quá endTime")
        void testAutoEndWhenTimeExpired() {
            // Người dùng đặt giá hợp lệ vượt giá sàn
            auction.placeBid(bidder1, 20000000.0);

            // Giả lập thời gian kết thúc đã trôi qua và cập nhật trạng thái
            auction.setEndTime(LocalDateTime.now().minusMinutes(5));
            auction.updateStatus();

            assertEquals(Status.FINISHED, auction.getStatus());
            assertEquals(bidder1, auction.getCurrentWinner());
        }
    }
}
