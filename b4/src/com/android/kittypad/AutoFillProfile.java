package com.android.kittypad;

public class AutoFillProfile {
        private int mUniqueId;
        private String mFullName;
        private String mEmailAddress;
        private String mCompanyName;
        private String mAddressLine1;
        private String mAddressLine2;
        private String mCity;
        private String mState;
        private String mZipCode;
        private String mCountry;
        private String mPhoneNumber;

        public AutoFillProfile(int uniqueId, String fullName, String email,
                String companyName, String addressLine1, String addressLine2,
                String city, String state, String zipCode, String country,
                String phoneNumber) {
            mUniqueId = uniqueId;
            mFullName = fullName;
            mEmailAddress = email;
            mCompanyName = companyName;
            mAddressLine1 = addressLine1;
            mAddressLine2 = addressLine2;
            mCity = city;
            mState = state;
            mZipCode = zipCode;
            mCountry = country;
            mPhoneNumber = phoneNumber;
        }

        public int getUniqueId() { return mUniqueId; }
        public String getFullName() { return mFullName; }
        public String getEmailAddress() { return mEmailAddress; }
        public String getCompanyName() { return mCompanyName; }
        public String getAddressLine1() { return mAddressLine1; }
        public String getAddressLine2() { return mAddressLine2; }
        public String getCity() { return mCity; }
        public String getState() { return mState; }
        public String getZipCode() { return mZipCode; }
        public String getCountry() { return mCountry; }
        public String getPhoneNumber() { return mPhoneNumber; }
    }
