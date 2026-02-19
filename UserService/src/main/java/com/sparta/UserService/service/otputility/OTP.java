package com.sparta.UserService.service.otputility;

import java.util.Random;

public class OTP {
    
    /**
     * Protected method to generate a 6-digit OTP
     * Can be accessed by derived classes (EmailOTP and SmsOTP)
     * 
     * @return 6-digit OTP as String
     */
    protected String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // Generates number between 100000 and 999999
        return String.valueOf(otp);
    }
}

