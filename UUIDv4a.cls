
/** Utility class for generation UUID version 4 (UUID based on random numbers)
  * Pattern: xxxxxxxx-xxxx-Mxxx-Nxxx-xxxxxxxxxxxx
  * M indicate the UUID version (4, random)
  * N indicate the UUID variant (2 most significant bits are 10)
  * https://en.wikipedia.org/wiki/Universally_unique_identifier
  * https://www.ietf.org/rfc/rfc4122.txt
  * See also https://github.com/pfrandsen/UUIDv4/blob/master/src/index.ts
  */
public class UUIDv4 {

    /**
     * Get UUID Version 4 variant 2
     */
    public static string getV4Variant2() {
        String hexChars = '0123456789abcdef';
        String variantChars = '89ab89ab89ab89ab';
        // Calculation of the varchar byte/hex character (top bits = '10') is based on the following
        // 0000 = 0  (0) -> 1000 -> '8'
        // 0001 = 1  (1) -> 1001 -> '9'
        // 0010 = 2  (2) -> 1010 -> 'a'
        // 0011 = 3  (3) -> 1011 -> 'b'
        // 0100 = 4  (4) -> 1000 -> '8'
        // 0101 = 5  (5) -> 1001 -> '9'
        // 0110 = 6  (6) -> 1010 -> 'a'
        // 0111 = 7  (7) -> 1011 -> 'b'
        // 1000 = 8  (8) -> 1000 -> '8'
        // 1001 = 9  (9) -> 1001 -> '9'
        // 1010 = a (10) -> 1010 -> 'a'
        // 1011 = b (11) -> 1011 -> 'b'
        // 1100 = c (12) -> 1000 -> '8'
        // 1101 = d (13) -> 1001 -> '9'
        // 1110 = e (14) -> 1010 -> 'a'
        // 1111 = f (15) -> 1011 -> 'b'
    
        Blob randomBits = Crypto.GenerateAESKey(128); // 128 bits, 16 bytes
        String hex = EncodingUtil.ConvertTohex(randomBits).toLowerCase();
        Integer idx = hexChars.indexOf(hex.SubString(12, 13));
        String uuid = hex.SubString(0,8) + '-' + hex.SubString(8,12) + '-4'
            + hex.SubString(13,16) + '-' + variantChars.substring(idx, idx + 1)
            + hex.SubString(17,20) + '-' + hex.substring(20);
        return uuid;
    }

}
