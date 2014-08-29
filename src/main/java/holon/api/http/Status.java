package holon.api.http;

public interface Status
{
    int code();

    enum Code implements Status
    {
        OK( 200 ),
        SEE_OTHER( 303 ),
        NOT_FOUND( 404 );

        private final int code;

        Code( int code )
        {
            this.code = code;
        }

        public int code()
        {
            return code;
        }

    }
}
