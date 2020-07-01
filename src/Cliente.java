import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;

public class Cliente extends JFrame implements ActionListener, KeyListener {

	private static final long serialVersionUID = 1L;
	private JTextArea texto;
	private JTextField txtMsg;
	private JButton btnSend;
	private JButton btnSair;
	private JLabel lblHistorico;
	private JLabel lblMsg;
	private JPanel pnlContent;
	private Socket socket;
	private OutputStream ou ;
	private Writer ouw; 
	private BufferedWriter bfw;
	private JTextField txtIP;
	private JTextField txtPorta;
	private JTextField txtNome;
	
	private String cpKey;
	private String csKey;
	
	private String kKey;
	
	private String spKey;

	public Cliente() throws IOException{                  
		JLabel lblMessage = new JLabel("Verificar!");
		txtIP = new JTextField("127.0.0.1");
		txtPorta = new JTextField("12345");
		txtNome = new JTextField("Cliente");                
		Object[] texts = {lblMessage, txtIP, txtPorta, txtNome };  
		JOptionPane.showMessageDialog(null, texts);              
		pnlContent = new JPanel();
		texto              = new JTextArea(10,20);
		texto.setEditable(false);
		texto.setBackground(new Color(240,240,240));
		txtMsg                       = new JTextField(20);
		lblHistorico     = new JLabel("Histórico");
		lblMsg        = new JLabel("Mensagem");
		btnSend                     = new JButton("Enviar");
		btnSend.setToolTipText("Enviar Mensagem");
		btnSair           = new JButton("Sair");
		btnSair.setToolTipText("Sair do Chat");
		btnSend.addActionListener(this);
		btnSair.addActionListener(this);
		btnSend.addKeyListener(this);
		txtMsg.addKeyListener(this);
		JScrollPane scroll = new JScrollPane(texto);
		texto.setLineWrap(true);  
		pnlContent.add(lblHistorico);
		pnlContent.add(scroll);
		pnlContent.add(lblMsg);
		pnlContent.add(txtMsg);
		pnlContent.add(btnSair);
		pnlContent.add(btnSend);
		pnlContent.setBackground(Color.LIGHT_GRAY);                                 
		texto.setBorder(BorderFactory.createEtchedBorder(Color.BLUE,Color.BLUE));
		txtMsg.setBorder(BorderFactory.createEtchedBorder(Color.BLUE, Color.BLUE));                    
		setTitle(txtNome.getText());
		setContentPane(pnlContent);
		setLocationRelativeTo(null);
		setResizable(false);
		setSize(250,300);
		setVisible(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
	
	private void generateRSAKeys() {
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(2048);
			KeyPair kp = kpg.generateKeyPair();
			
			Key pub = kp.getPublic();
			Key pvt = kp.getPrivate();
			
			cpKey = Base64.getEncoder().encodeToString(pub.getEncoded());
			csKey = Base64.getEncoder().encodeToString(pvt.getEncoded());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	public void conectar() {
		
		generateRSAKeys();
		
		try {
			socket = new Socket(txtIP.getText(),Integer.parseInt(txtPorta.getText()));
			ou = socket.getOutputStream();
			ouw = new OutputStreamWriter(ou);
			bfw = new BufferedWriter(ouw);
			
			bfw.write(txtNome.getText()+"\r\n" + cpKey +"\r\n");
			bfw.flush();
					
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String msgCriptografadaComKKey(String msg) throws Exception {
		byte[] decodedKey = Base64.getDecoder().decode(kKey);
		SecretKey secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
		
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(msg.getBytes("UTF-8")));
	}

	public void enviarMensagem(String msg) throws IOException{
		if(msg.equals("#sair")){
			bfw.write("Desconectado \r\n");
			texto.append("Desconectado \r\n");
			bfw.flush();
			txtMsg.setText("");
			bfw.close();
			ouw.close();
			ou.close();
			socket.close();
		} else {
			if (msg.contains("#nome ")) {
				String newName = msg.replace("#nome ", "");
				txtNome.setText(newName);
				setTitle(txtNome.getText());
			}
			
			try {
				
				if (!msg.contains("#kKey ")) {
					msg = msgCriptografadaComKKey(msg);
					texto.append( txtNome.getText() + " diz -> " + txtMsg.getText()+"\r\n");
					txtMsg.setText(""); 
				}
			
				bfw.write(msg+"\r\n");
				bfw.flush();
			
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public String msgDescriptografada(String msg) throws Exception {
		if (kKey != null) {
			System.out.println("Cliente transformou a mensagem criptografada " +msg);
			byte[] decodedKey = Base64.getDecoder().decode(kKey);
			SecretKey secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
			
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
		    cipher.init(Cipher.DECRYPT_MODE, secretKey);
		    String mensagemDescriptografada = new String(cipher.doFinal(Base64.getDecoder().decode(msg)));
			System.out.println("em mensagem descriptografada: " + mensagemDescriptografada);
		    return mensagemDescriptografada;
		} else {
			return msg;
		}
}

	public void escutar() {
		
		try {

		InputStream in = socket.getInputStream();
		InputStreamReader inr = new InputStreamReader(in);
		BufferedReader bfr = new BufferedReader(inr);
		String msg = "";

		while(!"#sair".equalsIgnoreCase(msg))
			if(bfr.ready()){
				try {
					msg = msgDescriptografada(bfr.readLine());
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (msg.contains("#spKey ")) {
					spKey = msg.replace("null -> #spKey ", "");
					System.out.println(txtNome.getText() + " recebeu spKey do servidor");
					System.out.println(spKey);
					try {
						kKey = generateKKey();
						System.out.println(txtNome.getText() + " gerou kKey");
						System.out.println(kKey);
						
						byte[] decodedKey = Base64.getDecoder().decode(spKey);
						
						X509EncodedKeySpec spec = new X509EncodedKeySpec(decodedKey);
						KeyFactory keyFactory = KeyFactory.getInstance("RSA");
						PublicKey rsaKey = keyFactory.generatePublic(spec);
						
						Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			            cipher.init(Cipher.ENCRYPT_MODE, rsaKey);
			            
			            System.out.println(txtNome.getText() + " criptografando chave K");
			            String encryptedKKey = Base64.getEncoder().encodeToString(cipher.doFinal(kKey.getBytes("UTF-8")));
						
						enviarMensagem("#kKey " + encryptedKKey);
						System.out.println(txtNome.getText() + " enviou kKey criptografada para servidor");
						System.out.println(encryptedKKey);
					} catch (Exception e) {
						e.printStackTrace();
					}

				} else {
					if(msg.equals("#sair"))
						texto.append("Servidor caiu! \r\n");
					else
						texto.append(msg+"\r\n");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String generateKKey() throws NoSuchAlgorithmException {
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(256);
		SecretKey secretKey = keyGen.generateKey();
		return Base64.getEncoder().encodeToString(secretKey.getEncoded());
	}

	public void sair() throws IOException{
		enviarMensagem("#sair");
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		try {
			if(e.getActionCommand().equals(btnSend.getActionCommand()))
				enviarMensagem(txtMsg.getText());
			else
				if(e.getActionCommand().equals(btnSair.getActionCommand()))
					sair();
		} catch (IOException e1) {
			e1.printStackTrace();
		}                       
	}

	@Override
	public void keyPressed(KeyEvent e) {

		if(e.getKeyCode() == KeyEvent.VK_ENTER){
			try {
				enviarMensagem(txtMsg.getText());
			} catch (IOException e1) {
				e1.printStackTrace();
			}                                                          
		}                       
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
		// TODO Auto-generated method stub               
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
		// TODO Auto-generated method stub               
	}

	public static void main(String []args) throws IOException{

		Cliente app = new Cliente();
		app.conectar();
		app.escutar();
	}

}
