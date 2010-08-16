package net.osmand;

/**
 * @source http://www.javaworld.com/javaworld/javatips/jw-javatip47.html -- 24.11.2008, (mb)
 */
public class Base64 {
        /*******************************************************************************************************************
         * BASE 64 encoding of a String or an array of bytes. See also RFC 1421.
         *
         * @author Unknown
         * @author David W. Croft
         * @version 1998-06-08
         ******************************************************************************************************************/

        //////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////
        public static final char[] alphabet = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', //  0 to  7
                        'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', //  8 to 15
                        'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', // 16 to 23
                        'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', // 24 to 31
                        'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', // 32 to 39
                        'o', 'p', 'q', 'r', 's', 't', 'u', 'v', // 40 to 47
                        'w', 'x', 'y', 'z', '0', '1', '2', '3', // 48 to 55
                        '4', '5', '6', '7', '8', '9', '+', '/' }; // 56 to 63

        //////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////

        public static String encode(final String s)
        //////////////////////////////////////////////////////////////////////
        {
                return encode(s.getBytes());
        }

        public static String encode(final byte[] octetString)
        //////////////////////////////////////////////////////////////////////
        {
                int bits24;
                int bits6;

                final char[] out = new char[((octetString.length - 1) / 3 + 1) * 4];

                int outIndex = 0;
                int i = 0;

                while ((i + 3) <= octetString.length) {
                        // store the octets
                        bits24 = (octetString[i++] & 0xFF) << 16;
                        bits24 |= (octetString[i++] & 0xFF) << 8;
                        bits24 |= (octetString[i++] & 0xFF) << 0;

                        bits6 = (bits24 & 0x00FC0000) >> 18;
                        out[outIndex++] = alphabet[bits6];
                        bits6 = (bits24 & 0x0003F000) >> 12;
                        out[outIndex++] = alphabet[bits6];
                        bits6 = (bits24 & 0x00000FC0) >> 6;
                        out[outIndex++] = alphabet[bits6];
                        bits6 = (bits24 & 0x0000003F);
                        out[outIndex++] = alphabet[bits6];
                }

                if (octetString.length - i == 2) {
                        // store the octets
                        bits24 = (octetString[i] & 0xFF) << 16;
                        bits24 |= (octetString[i + 1] & 0xFF) << 8;

                        bits6 = (bits24 & 0x00FC0000) >> 18;
                        out[outIndex++] = alphabet[bits6];
                        bits6 = (bits24 & 0x0003F000) >> 12;
                        out[outIndex++] = alphabet[bits6];
                        bits6 = (bits24 & 0x00000FC0) >> 6;
                        out[outIndex++] = alphabet[bits6];

                        // padding
                        out[outIndex++] = '=';
                } else if (octetString.length - i == 1) {
                        // store the octets
                        bits24 = (octetString[i] & 0xFF) << 16;

                        bits6 = (bits24 & 0x00FC0000) >> 18;
                        out[outIndex++] = alphabet[bits6];
                        bits6 = (bits24 & 0x0003F000) >> 12;
                        out[outIndex++] = alphabet[bits6];

                        // padding
                        out[outIndex++] = '=';
                        out[outIndex++] = '=';
                }

                return new String(out);
        }

}