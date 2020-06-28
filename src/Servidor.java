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
import java.util.ArrayList;
import java.util.Base64;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

public class Servidor extends Thread {

	private static ArrayList<BufferedWriter>clientes;           
	private static ServerSocket server; 
	private String nome;
	private String chave;
	private Socket con;
	private InputStream in;  
	private InputStreamReader inr;  
	private BufferedReader bfr;
	private static boolean sendKey = false;
	
	private static String spKey;
	private static String ssKey;
	
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
			Key pvt = kp.getPrivate();
			
			spKey = Base64.getEncoder().encodeToString(pub.getEncoded());
			ssKey = Base64.getEncoder().encodeToString(pvt.getEncoded());
			
			System.out.println("chave do servidor gerada: ");
			System.out.println(spKey);
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	public void run(){
		
		try{
//			System.out.println("1");
			String msg;
			OutputStream ou =  this.con.getOutputStream();
			Writer ouw = new OutputStreamWriter(ou);
			BufferedWriter bfw = new BufferedWriter(ouw); 
			clientes.add(bfw);
			
			if (sendKey) {
				msg = "#chave " + spKey;					
				sendToNewClient(msg);
				sendKey = false;
			}
			
//			System.out.println("2");
			
			nome = msg = bfr.readLine();
			chave = bfr.readLine();
			
			System.out.println("Servidor recebeu chave de " + nome);
			System.out.println(chave);
			
			while(!"Sair".equalsIgnoreCase(msg) && msg != null) {    
//				System.out.println("3");
				if (msg.contains("#nome ")) {
					String newName = msg.replace("#nome ", "");
					nome = newName;
				}
				msg = bfr.readLine();					
				sendToAll(bfw, msg);
			}

		}catch (Exception e) {
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

	public void sendToAll(BufferedWriter bwSaida, String msg) {
		BufferedWriter bwS;

		for(BufferedWriter bw : clientes){
			bwS = (BufferedWriter)bw;
			if(!(bwSaida == bwS)){
				try {
				bw.write(nome + " -> " + msg+"\r\n");
				bw.flush(); 
				} catch (IOException e) {
					e.printStackTrace();
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

