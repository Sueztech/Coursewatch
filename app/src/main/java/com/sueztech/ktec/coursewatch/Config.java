package com.sueztech.ktec.coursewatch;

class Config {

    class Urls {

        private static final String PROTO = "https://";
        private static final String KTEC_API_URL = PROTO + "ktec.sueztech.com/api/";

        class Static {
            private static final String PREFIX = KTEC_API_URL + "static_";
            static final String COLLEGE_LIST = PREFIX + "colleges.php";
        }

        class Sso {
            private static final String PREFIX = KTEC_API_URL + "sso_";
            static final String SIGNUP = PREFIX + "signup.php";
            static final String LOGIN = PREFIX + "login.php";
            static final String STATUS = PREFIX + "status.php";
        }

        class User {
            private static final String PREFIX = KTEC_API_URL + "user_";
            static final String NAME = PREFIX + "name.php";
            static final String EMAIL = PREFIX + "email.php";
        }

    }

}
