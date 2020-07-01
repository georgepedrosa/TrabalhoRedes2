import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

public class Servidor extends Thread {

	private static ArrayList<BufferedWriter>clientes;           
	private static ServerSocket server; 
	private String nome;
	
	private String cpKey;
	private String kKey;
	
	private Socket con;
	private InputStream in;  
	private InputStreamReader inr;  
	private BufferedReader bfr;
	private static boolean sendKey = false;
	
	private static String spKey;
	private static PrivateKey ssKey;
	
	private static List<String> kKeys = new ArrayList<String>();
	
	public Servidor(Socket con){
		this.con = con;
		try {
			in  = con.getInputStream();
			inr = new InputStreamReader(in);
			bfr = new BufferedReader(inr);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void generateRSAKeys() {

		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(2048);
			KeyPair kp = kpg.generateKeyPair();
			
			Key pub = kp.getPublic();
			spKey = Base64.getEncoder().encodeToString(pub.getEncoded());

			ssKey = kp.getPrivate();
			
			System.out.println("spKey do servidor gerada: ");
			System.out.println(spKey);
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	public void run(){
		
		try{
			String msg;
			OutputStream ou =  this.con.getOutputStream();
			Writer ouw = new OutputStreamWriter(ou);
			BufferedWriter bfw = new BufferedWriter(ouw); 
			clientes.add(bfw);
			
			if (sendKey) {
				msg = "#spKey " + spKey;					
				sendToNewClient(msg);
				sendKey = false;
			}
						
			nome = msg = bfr.readLine();
			cpKey = bfr.readLine();
			
			System.out.println("Servidor recebeu cpKey de " + nome);
			System.out.println(cpKey);
			
			while(!"#sair".equalsIgnoreCase(msg) && msg != null) {    
				if (msg.contains("#nome ")) {
					String newName = msg.replace("#nome ", "");
					nome = newName;
				} else if (msg.contains("#kKey ")) {
					String encryptedKKey = msg.replace("#kKey ", "");
					
					System.out.println("Servidor recebeu chave K criptografada: " + encryptedKKey);
					
					byte[] decodedEncryptedKKeyKey = Base64.getDecoder().decode(encryptedKKey);
					
			        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			        cipher.init(Cipher.DECRYPT_MODE, ssKey);
			        
			        System.out.println("Descriptografando chave K");
					
			        kKey = new String(cipher.doFinal(decodedEncryptedKKeyKey));
					kKeys.add(new String(cipher.doFinal(decodedEncryptedKKeyKey))); 
					
					System.out.println("Servidor salvou kKey: " + kKeys.get(kKeys.size()-1));
				}
				msg = bfr.readLine();					
				sendToAll(bfw, msg);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}                       
	}
	
	public void sendToNewClient(String msg) {
		BufferedWriter bw = clientes.get(clientes.size()-1);
		try {
		bw.write(nome + " -> " + msg+"\r\n");
		bw.flush(); 
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String msgDescriptografada(String msg) throws Exception {
		byte[] decodedKey = Base64.getDecoder().decode(kKey);
		SecretKey secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
		
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return new String(cipher.doFinal(Base64.getDecoder().decode(msg)));
	}
	
	public String msgCriptografadaComKKeyDoCliente(String msg, int index) throws Exception {		
		byte[] decodedKey = Base64.getDecoder().decode(kKeys.get(index));
		SecretKey secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
		
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(msg.getBytes("UTF-8")));
	}

	public void sendToAll(BufferedWriter bwSaida, String msg) {
		String msgDescriptografada = msg;
		
		if (kKey != null) {
			try {
				msgDescriptografada = msgDescriptografada(msg);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if (!msg.contains("#kKey ")) {
			BufferedWriter bwS;
	
			for(int i=0; i<clientes.size();i++) {
				BufferedWriter bw = clientes.get(i);
				bwS = (BufferedWriter)bw;
				if(!(bwSaida == bwS)){
					try {
						String line = nome + " -> " + msgDescriptografada;
						String mensagemCriptografada = msgCriptografadaComKKeyDoCliente(line, i);
						System.out.println("Mensagem criptografada enviada pelo servidor: " + mensagemCriptografada);
						bw.write(mensagemCriptografada + "\r\n");
						bw.flush(); 
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public static void main(String []args) {

		try{
			generateRSAKeys();
			JLabel lblMessage = new JLabel("Porta do Servidor:");
			JTextField txtPorta = new JTextField("12345");
			Object[] texts = {lblMessage, txtPorta };  
			JOptionPane.showMessageDialog(null, texts);
			server = new ServerSocket(Integer.parseInt(txtPorta.getText()));
			clientes = new ArrayList<BufferedWriter>();
			JOptionPane.showMessageDialog(null,"Servidor ativo na porta: "+         
					txtPorta.getText());

			while(true){
				System.out.println("Aguardando conexão...");
				Socket con = server.accept();
				System.out.println("Cliente conectado...");
				Thread t = new Servidor(con);
				sendKey = true;
				t.start();  

			}

		}catch (Exception e) {

			e.printStackTrace();
		}                       
	}                      
}

