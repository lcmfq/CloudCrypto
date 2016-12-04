package com.example.encryption.abe;

import cn.edu.buaa.crypto.access.parser.PolicySyntaxException;
import cn.edu.buaa.crypto.algebra.serparams.PairingCipherSerParameter;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerPair;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;
import cn.edu.buaa.crypto.encryption.abe.cpabe.CPABEEngine;
import cn.edu.buaa.crypto.encryption.abe.cpabe.rw13.CPABERW13Engine;
import cn.edu.buaa.crypto.utils.Timer;
import com.example.TestUtils;
import edu.princeton.cs.algs4.Out;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import junit.framework.TestCase;
import org.bouncycastle.crypto.InvalidCipherTextException;

/**
 * Created by Weiran Liu on 2016/12/4.
 *
 * CP-ABE engine performance test.
 */
public class CPABEPerformanceTest {
    private String pairingParameterPath;
    //file path for performance test result
    private static final String default_path = "benchmarks/encryption/cpabe/";
    //test round
    private int test_round;
    //the maximal number of role index is chosen
    private static final int maximal_attributes = 10;
    //setup time
    private double timeSetep;

    //attributeSets
    private String[][] attributeSets = new String[maximal_attributes][];
    //secret key generation time
    private double[] timeKeyGen = new double[maximal_attributes];

    //access policy
    private String[] accessPolicies = new String[maximal_attributes];
    //key encapsulation time
    private double[] timeEncapsulation = new double[maximal_attributes];
    //key encryption time
    private double[] timeEncryption = new double[maximal_attributes];

    //decapsulation time
    private double[] timeDecapsulation = new double[maximal_attributes];
    private double[] timeDecryption = new double[maximal_attributes];

    private CPABEEngine engine;

    private Out out;

    public CPABEPerformanceTest() {
        //create attributeSets
        for (int i = 0; i < maximal_attributes; i++){
            this.attributeSets[i] = new String[i+1];
        }
        for (int i = 0; i < maximal_attributes; i++){
            for (int j = 0; j <= i; j++){
                this.attributeSets[i][j] = "A_" + (j);
            }
        }

//        for (int i = 0; i < this.attributeSets.length; i++) {
//            System.out.print("i = " + i + ": ");
//            for (int j = 0; j < this.attributeSets[i].length; j++) {
//                System.out.print(this.attributeSets[i][j] + " ");
//            }
//            System.out.println();
//        }

        //create access policies for encapsulation / encryption
        for (int i = 0; i < maximal_attributes; i++) {
            this.accessPolicies[i] = "";
        }
        for (int i = 0; i < maximal_attributes; i++){
            for (int j = 0; j < i; j++){
                this.accessPolicies[i] += "A_" + (j) + " AND ";
            }
            this.accessPolicies[i] += "A_" + (i);
        }
//        for (int i = 0; i < this.accessPolicies.length; i++) {
//            System.out.println(this.accessPolicies[i]);
//        }
    }

    private void runPerformanceTest() {
        out = new Out(default_path + engine.getEngineName());
        out.println("Test CP-ABE engine: " + engine.getEngineName());
        out.println("All test rounds: " + this.test_round);

        for (int i = 0; i < test_round; i++) {
            System.out.println("Test round: " + (i+1));
            out.println("Test round: " + (i+1));
            run_one_round();
        }
        //write results to the file
        //write setup time
        out.print("Setup : ");
        out.print("\t" + this.timeSetep / test_round);
        out.println();

        //write KeyGen
        out.print("KeyGen: ");
        for (int i = 0; i < maximal_attributes; i++) {
            out.print("\t" + this.timeKeyGen[i] / test_round);
        }
        out.println();

        //write encapsulation
        out.print("Encapsulation: ");
        for (int i = 0; i < maximal_attributes; i++) {
            out.print("\t" + this.timeEncapsulation[i] / test_round);
        }
        out.println();

        //write encryption
        out.print("Encryption: ");
        for (int i = 0; i < maximal_attributes; i++) {
            out.print("\t" + this.timeEncryption[i] / test_round);
        }
        out.println();

        //write decryption
        out.print("Decryption: ");
        for (int i = 0; i < maximal_attributes; i++) {
            out.print("\t" + this.timeDecryption[i] / test_round);
        }
        out.println();

        //write decapsulation
        out.print("Decapsulation: ");
        for (int i = 0; i < maximal_attributes; i++) {
            out.print("\t" + this.timeDecapsulation[i] / test_round);
        }
        out.println();
    }

    private void run_one_round() {
        try {
            PairingParameters pairingParameters = PairingFactory.getPairingParameters(pairingParameterPath);
            Pairing pairing = PairingFactory.getPairing(pairingParameters);

            double temperTime;
            Timer timer = new Timer(maximal_attributes);
            //test setup performance
            System.out.print("Setup; ");
            out.print("Setup : ");
            timer.start(0);
            PairingKeySerPair keyPair = engine.setup(pairingParameters, maximal_attributes);
            temperTime = timer.stop(0);
            out.print("\t" + temperTime);
            this.timeSetep += temperTime;
            out.println();
            System.out.println();

            PairingKeySerParameter publicKey = keyPair.getPublic();
            PairingKeySerParameter masterKey = keyPair.getPrivate();

            out.print("KeyGen: ");
            //test secret key generation performance
            PairingKeySerParameter[] secretKeys = new PairingKeySerParameter[maximal_attributes];
            for (int i = 0; i < maximal_attributes; i++) {
                System.out.print("KeyGen " + i + "; ");
                timer.start(i);
                secretKeys[i] = engine.keyGen(publicKey, masterKey, attributeSets[i]);
                temperTime = timer.stop(i);
                out.print("\t" + temperTime);
                this.timeKeyGen[i] += temperTime;
            }
            out.println();
            System.out.println();

            out.print("Encryption: ");
            //test encryption performance
            PairingCipherSerParameter[] ciphertexts = new PairingCipherSerParameter[maximal_attributes];
            for (int i = 0; i < maximal_attributes; i++) {
                Element message = pairing.getGT().newRandomElement().getImmutable();
                System.out.print("Encryption " + i + "; ");
                timer.start(i);
                ciphertexts[i] = engine.encryption(publicKey, accessPolicies[i], message);
                temperTime = timer.stop(i);
                out.print("\t" + temperTime);
                this.timeEncryption[i] += temperTime;
            }
            out.println();
            System.out.println();

            out.print("Encapsulation: ");
            //test encryption performance
            PairingCipherSerParameter[] headers = new PairingCipherSerParameter[maximal_attributes];
            for (int i = 0; i < maximal_attributes; i++) {
                System.out.print("Encapsulation " + i + "; ");
                timer.start(i);
                headers[i] = engine.encapsulation(publicKey, accessPolicies[i]).getHeader();
                temperTime = timer.stop(i);
                out.print("\t" + temperTime);
                this.timeEncapsulation[i] += temperTime;
            }
            out.println();
            System.out.println();

            //test decryption performance
            out.print("Decryption; ");
            for (int i = 0; i < maximal_attributes; i++) {
                System.out.print("Decryption " + i + "; ");
                timer.start(i);
                engine.decryption(publicKey, secretKeys[i], accessPolicies[i], ciphertexts[i]);
                temperTime = timer.stop(i);
                out.print("\t" + temperTime);
                this.timeDecryption[i] += temperTime;
            }
            out.println();
            System.out.println();

            out.print("Decapsulation: ");
            //test decapsulation performance
            for (int i = 0; i < maximal_attributes; i++) {
                System.out.print("Decapsulation " + i + "; ");
                timer.start(i);
                engine.decapsulation(publicKey, secretKeys[i], accessPolicies[i], headers[i]);
                temperTime = timer.stop(i);
                out.print("\t" + temperTime);
                this.timeDecapsulation[i] += temperTime;
            }
            out.println();
            System.out.println();
        } catch (InvalidCipherTextException e) {
            e.printStackTrace();
        } catch (PolicySyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        CPABEPerformanceTest cpabePerformanceTest = new CPABEPerformanceTest();
        cpabePerformanceTest.pairingParameterPath = TestUtils.TEST_PAIRING_PARAMETERS_PATH_a1_3_128;
        cpabePerformanceTest.test_round = TestUtils.DEFAULT_SIMU_TEST_ROUND;
        cpabePerformanceTest.engine = CPABERW13Engine.getInstance();
        cpabePerformanceTest.runPerformanceTest();
    }
}
