import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import util.JSONUtils;
import zhengfang.ZFClient;

public class LoginFrame extends JFrame {

	private JTextField account = new JTextField("0702110116");
	private JPasswordField password = new JPasswordField("320586199211074855");
	private JLabel checkCodeImage = new JLabel();
	private JTextField checkCode = new JTextField(5);

	private JButton submit = new JButton("login");
	private JButton exit = new JButton("exit");

	private ZFClient client = new ZFClient(2);

	public LoginFrame() throws IOException, Exception {
		// TODO 自动生成的构造函数存根

		initUi();
		initListener();
	}

	private void initUi() throws IOException, Exception {
		setSize(500, 300);
		setLocationRelativeTo(null);
		setTitle("正方教务系统");

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new GridLayout(2, 1, 5, 2));

		JPanel panel1 = new JPanel();
		panel1.add(new JLabel("account"));
		panel1.add(account);

		JPanel panel2 = new JPanel();
		panel2.add(new JLabel("password"));
		panel2.add(password);

		// JPanel panel3 = new JPanel();
		// panel3.add(new JLabel("checkCode"));
		// panel3.add(checkCode);
		// panel3.add(checkCodeImage);

		mainPanel.add(panel1);
		mainPanel.add(panel2);
		// mainPanel.add(panel3);
		add(mainPanel, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(1, 2));
		buttonPanel.add(submit);
		buttonPanel.add(exit);
		add(buttonPanel, BorderLayout.SOUTH);
        client.initConnection();
		//client.getCheckCodeInputStream();
		// checkCodeImage.setIcon(new ImageIcon(ImageIO.read(client
		// .getCheckCodeInputStream())));

	}

	private void initListener() {
		/*checkCodeImage.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent mv) {
				// TODO 自动生成的方法存根
				try {
					checkCodeImage.setIcon(new ImageIcon(ImageIO.read(client
							.getCheckCodeInputStream())));
				} catch (Exception e) {
					// TODO 自动生成的 catch 块
					e.printStackTrace();
				}
			}
		});*/

		submit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent ae) {
				// TODO 自动生成的方法存根

				try {
					boolean ok = client.login(account.getText(),
							password.getText());
					ok = client.login(account.getText(), password.getText());

					if (ok == false)
						return;

					System.out.println(JSONUtils.getStudentInfoJson(client
							.getStudentInfo()));
					System.out.println(JSONUtils.getTimeTableJson(client
							.getTimeTable()));
					System.out.println(JSONUtils.getReportCardJson(client
							.getReportCard("2013-2014", "1")));
				} catch (Exception e) {
					// TODO 自动生成的 catch 块
					e.printStackTrace();
				}

			}
		});

	}

	public static void main(String[] args) throws Exception {
		LoginFrame frame = new LoginFrame();
		frame.setVisible(true);
	}
}
