package systems.symbol.trust;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * Bootstrap a Trust Chain
 *
 * create the Java Keystore as the wallet
 *
 * create the sovereign key
 * create 5 guardians keys
 * two are cold / dormant
 * three are designated custodians - majeur, disaster and governance.
 * Governance is used through the trust chain operations
 * each custodian is authorized as an intermediate certified authority
 *
 */

public class Genesis {

    public static void main(String [] args){
        String certFile = "cacert.pem";
        String keyFile = "cakey.p8c";
        String message = "This is my secret message.";

        try {
            InputStream certStream = new FileInputStream(certFile);
            InputStream keyStream= new FileInputStream(keyFile);
            Locksmith locksmith = new Locksmith();
            locksmith.encrypt(certStream, keyStream, message);
            certStream.close();
            keyStream.close();

        }catch( IOException e ){
            System.out.println( "IOException:" + e );
        }catch( CertificateException e ){
            System.out.println( "CertificateException:" + e );
        }catch( NoSuchAlgorithmException e ){
            System.out.println( "NoSuchAlgorithmException:" + e );
        } catch (Exception e) {
            System.out.println( "Exception:" + e );
        }
    }


}
