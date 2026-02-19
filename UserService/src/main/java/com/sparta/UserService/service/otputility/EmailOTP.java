package com.sparta.UserService.service.otputility;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailOTP extends OTP {
    
    @Autowired
    private JavaMailSender mailSender;
    
    /**
     * Sends OTP via email using Google Mail service
     * 
     * @param email Recipient email address
     * @return Generated 6-digit OTP
     */
    public String sendOTP(String email) {
        String otp = generateOTP(); // Accessing protected method from parent class
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Your OTP Verification Code");
        message.setText("Your OTP verification code is: " + otp + "\n\nThis code will expire in 5 minutes.");
        
        mailSender.send(message);
        
        return otp;
    }
    
    /**
     * Sends OTP via email with custom subject and message
     * 
     * @param email Recipient email address
     * @param subject Email subject
     * @param customMessage Custom message to include with OTP
     * @return Generated 6-digit OTP
     */
    public String sendOTP(String email, String subject, String customMessage) {
        String otp = generateOTP(); // Accessing protected method from parent class
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subject);
        message.setText(customMessage + "\n\nYour OTP verification code is: " + otp + "\n\nThis code will expire in 5 minutes.");
        
        mailSender.send(message);
        
        return otp;
    }
}

