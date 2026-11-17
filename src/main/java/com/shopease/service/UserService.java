package com.shopease.service;

import com.shopease.dao.UserDao;
import com.shopease.exception.BusinessException;
import com.shopease.exception.NotFoundException;
import com.shopease.model.Role;
import com.shopease.model.User;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern PHONE = Pattern.compile("^[0-9+\\- ]{7,15}$");

    private final UserDao userDao;
    private final PasswordEncoder encoder;

    public UserService(UserDao userDao, PasswordEncoder encoder) {
        this.userDao = userDao;
        this.encoder = encoder;
    }

    public User register(String fullName, String email, String phone, String password, String confirm) {
        if (fullName == null || fullName.isBlank()) throw new BusinessException("Please enter your name.");
        if (fullName.length() > 120) throw new BusinessException("Name is too long.");
        String normEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(normEmail).matches()) throw new BusinessException("Please enter a valid email address.");
        if (phone != null && !phone.isBlank() && !PHONE.matcher(phone.trim()).matches()) {
            throw new BusinessException("Please enter a valid phone number.");
        }
        validatePassword(password, confirm);
        if (userDao.findByEmail(normEmail).isPresent()) throw new BusinessException("An account with this email already exists.");

        User u = new User();
        u.setFullName(fullName.trim());
        u.setEmail(normEmail);
        u.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        u.setRole(Role.CUSTOMER);
        u.setPasswordHash(encoder.encode(password));
        try {
            userDao.insert(u);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("An account with this email already exists.");
        }
        return u;
    }

    /** Returns the user when the email/password pair is valid and the account is active. */
    public Optional<User> authenticate(String email, String password) {
        if (email == null || password == null) return Optional.empty();
        return userDao.findByEmail(email.trim())
                .filter(User::isActive)
                .filter(u -> encoder.matches(password, u.getPasswordHash()));
    }

    public User get(long id) {
        return userDao.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    public List<User> listAll() {
        return userDao.findAll();
    }

    public void updateProfile(long id, String fullName, String phone) {
        if (fullName == null || fullName.isBlank()) throw new BusinessException("Name cannot be empty.");
        if (phone != null && !phone.isBlank() && !PHONE.matcher(phone.trim()).matches()) {
            throw new BusinessException("Please enter a valid phone number.");
        }
        userDao.updateProfile(id, fullName.trim(), phone == null || phone.isBlank() ? null : phone.trim());
    }

    public void changePassword(long id, String current, String password, String confirm) {
        User u = get(id);
        if (current == null || !encoder.matches(current, u.getPasswordHash())) throw new BusinessException("Current password is incorrect.");
        validatePassword(password, confirm);
        userDao.updatePassword(id, encoder.encode(password));
    }

    public void setActive(long id, boolean active, long actingAdminId) {
        if (id == actingAdminId) throw new BusinessException("You cannot deactivate your own account.");
        get(id);
        userDao.setActive(id, active);
    }

    public long countCustomers() {
        return userDao.count();
    }

    private static void validatePassword(String password, String confirm) {
        if (password == null || password.length() < 8) throw new BusinessException("Password must be at least 8 characters.");
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new BusinessException("Password must contain both letters and numbers.");
        }
        if (!password.equals(confirm)) throw new BusinessException("Passwords do not match.");
    }
}
