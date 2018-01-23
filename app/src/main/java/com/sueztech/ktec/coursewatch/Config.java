package com.sueztech.ktec.coursewatch;

@SuppressWarnings("WeakerAccess")
class Config {

    static final String PROTO = "https://";
    static final String SSO_API_URL = PROTO + "ktec.sueztech.com/api/";

    static final String SSO_COLLEGES_URL = SSO_API_URL + "colleges.php";
    static final String SSO_SIGNUP_URL = SSO_API_URL + "signup.php";

    static final String SSO_LOGIN_URL = SSO_API_URL + "login.php";

}
