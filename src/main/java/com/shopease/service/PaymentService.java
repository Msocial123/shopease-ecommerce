package com.shopease.service;

import com.shopease.dao.OrderDao;
import com.shopease.dao.PaymentDao;
import com.shopease.exception.BusinessException;
import com.shopease.exception.NotFoundException;
import com.shopease.model.Order;
import com.shopease.model.OrderStatus;
import com.shopease.model.Payment;
import com.shopease.model.PaymentMethod;
import com.shopease.model.PaymentStatus;
import com.shopease.model.SessionUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Payment Service backed by a <b>simulated</b> payment gateway (no real money moves).
 *
 * <p>Test data:</p>
 * <ul>
 *     <li>Card: any Luhn-valid number, future expiry, 3-4 digit CVV succeeds, e.g. 4111 1111 1111 1111.
 *         A card ending in <code>0002</code> (e.g. 4000 0000 0000 0002) is declined.</li>
 *     <li>UPI: any id like <code>name@bank</code> succeeds; <code>fail@upi</code> is declined.</li>
 *     <li>Net banking: choose any bank.</li>
 *     <li>Cash on delivery: order is placed, payment collected on delivery (max order value 50,000).</li>
 * </ul>
 * To integrate a real gateway (Razorpay, Stripe, PayU...), replace {@link #gatewayCharge}.
 */
@Service
public class PaymentService {

    public static final BigDecimal COD_LIMIT = new BigDecimal("50000");
    public static final List<String> BANKS = List.of(
            "State Bank of India", "HDFC Bank", "ICICI Bank", "Axis Bank", "Kotak Mahindra Bank", "Punjab National Bank");

    private static final Pattern UPI = Pattern.compile("^[a-zA-Z0-9._-]{2,64}@[a-zA-Z]{2,32}$");
    private static final Pattern EXPIRY = Pattern.compile("^(0[1-9]|1[0-2])\\s*/\\s*(\\d{2})$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderDao orderDao;
    private final PaymentDao paymentDao;

    public PaymentService(OrderDao orderDao, PaymentDao paymentDao) {
        this.orderDao = orderDao;
        this.paymentDao = paymentDao;
    }

    /** Result of a gateway call. */
    public record GatewayResult(boolean approved, String message) {
    }

    /**
     * Pays for an order. Input-format errors throw {@link BusinessException} (nothing recorded);
     * gateway declines are recorded as FAILED payments and returned so the user can retry.
     */
    @Transactional
    public Payment pay(String orderNumber, SessionUser user, PaymentRequest req) {
        Order order = orderDao.findByNumberForUpdate(orderNumber).orElseThrow(() -> new NotFoundException("Order not found"));
        if (!order.getUserId().equals(user.getId())) throw new NotFoundException("Order not found");
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException("Order " + orderNumber + " is not awaiting payment.");
        }
        if (req.getMethod() == null) throw new BusinessException("Please choose a payment method.");

        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setTransactionRef(newTransactionRef());
        payment.setMethod(req.getMethod());
        payment.setAmount(order.getTotal());
        payment.setCreatedAt(LocalDateTime.now());

        GatewayResult result;
        switch (req.getMethod()) {
            case CARD -> {
                String digits = validateCard(req);
                payment.setCardLast4(digits.substring(digits.length() - 4));
                payment.setCardBrand(cardBrand(digits));
                result = gatewayCharge(req.getMethod(), digits, order.getTotal());
            }
            case UPI -> {
                String upi = req.getUpiId() == null ? "" : req.getUpiId().trim().toLowerCase();
                if (!UPI.matcher(upi).matches()) throw new BusinessException("Please enter a valid UPI ID, e.g. yourname@okbank");
                payment.setUpiId(upi);
                result = gatewayCharge(req.getMethod(), upi, order.getTotal());
            }
            case NETBANKING -> {
                if (req.getBank() == null || !BANKS.contains(req.getBank())) throw new BusinessException("Please select your bank.");
                result = gatewayCharge(req.getMethod(), req.getBank(), order.getTotal());
            }
            case COD -> {
                if (order.getTotal().compareTo(COD_LIMIT) > 0) {
                    throw new BusinessException("Cash on Delivery is available only for orders up to 50,000.");
                }
                payment.setStatus(PaymentStatus.PENDING);
                payment.setMessage("To be collected on delivery");
                paymentDao.insert(payment);
                orderDao.updatePaymentMethod(order.getId(), PaymentMethod.COD);
                orderDao.updateStatus(order.getId(), OrderStatus.PLACED);
                return payment;
            }
            default -> throw new BusinessException("Unsupported payment method.");
        }

        payment.setStatus(result.approved() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        payment.setMessage(result.message());
        paymentDao.insert(payment);
        if (result.approved()) {
            orderDao.updatePaymentMethod(order.getId(), req.getMethod());
            orderDao.updateStatus(order.getId(), OrderStatus.PAID);
        }
        return payment;
    }

    /** Simulated acquirer. Swap this method for a real gateway SDK call. */
    protected GatewayResult gatewayCharge(PaymentMethod method, String instrument, BigDecimal amount) {
        if (method == PaymentMethod.CARD && instrument.endsWith("0002")) {
            return new GatewayResult(false, "Card declined by issuing bank");
        }
        if (method == PaymentMethod.UPI && instrument.startsWith("fail@")) {
            return new GatewayResult(false, "UPI request declined by payer");
        }
        return new GatewayResult(true, "Approved");
    }

    // ------------------------------------------------------------------ card validation

    /** Validates the card and returns the digits-only number. */
    String validateCard(PaymentRequest req) {
        if (req.getCardName() == null || req.getCardName().isBlank()) throw new BusinessException("Name on card is required.");
        String digits = req.getCardNumber() == null ? "" : req.getCardNumber().replaceAll("[\\s-]", "");
        if (!digits.matches("\\d{13,19}") || !luhnValid(digits)) throw new BusinessException("Please enter a valid card number.");
        Matcher m = EXPIRY.matcher(req.getCardExpiry() == null ? "" : req.getCardExpiry().trim());
        if (!m.matches()) throw new BusinessException("Expiry must be in MM/YY format.");
        YearMonth expiry = YearMonth.parse("20" + m.group(2) + "-" + m.group(1), DateTimeFormatter.ofPattern("yyyy-MM"));
        if (expiry.isBefore(YearMonth.now())) throw new BusinessException("This card has expired.");
        String cvv = req.getCardCvv() == null ? "" : req.getCardCvv().trim();
        int cvvLen = digits.startsWith("34") || digits.startsWith("37") ? 4 : 3;
        if (!cvv.matches("\\d{" + cvvLen + "}")) throw new BusinessException("Please enter a valid " + cvvLen + "-digit CVV.");
        return digits;
    }

    public static boolean luhnValid(String digits) {
        int sum = 0;
        boolean alt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alt) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alt = !alt;
        }
        return sum % 10 == 0;
    }

    public static String cardBrand(String digits) {
        if (digits.startsWith("4")) return "VISA";
        if (digits.startsWith("34") || digits.startsWith("37")) return "AMEX";
        if (digits.matches("^(5[1-5]|222[1-9]|22[3-9]\\d|2[3-6]\\d{2}|27[01]\\d|2720).*")) return "MASTERCARD";
        if (digits.matches("^(60|65|81|82|508).*")) return "RUPAY";
        return "CARD";
    }

    private static String newTransactionRef() {
        return "TXN" + System.currentTimeMillis() + String.format("%04d", RANDOM.nextInt(10_000));
    }
}
