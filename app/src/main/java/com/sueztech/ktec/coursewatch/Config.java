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

    }

}
