import javax.swing.*;
import java.awt.*;

public class WebBenchGUI {
    public static void main(String[] args){
        JFrame f=new JFrame("WebBench");
        f.setSize(800,600);
        f.setLocation(200,200);
        f.setLayout(null);
        JPanel pLeft = new JPanel();
        pLeft.setBounds(50,50,300,60);
        pLeft.setLayout(new FlowLayout());

        JLabel urlLabel = new JLabel("URL:");
        pLeft.add(urlLabel);
        JTextField urlText = new JTextField("请输入目标url");
        urlText.setPreferredSize(new Dimension(80, 30));
        pLeft.add(urlText);

        JButton checkButton = new JButton("Check");
        pLeft.add(checkButton);
        JCheckBox forceCheck = new JCheckBox("-f|--force  Don't wait for reply from server.");
        forceCheck.setSelected(true);
        pLeft.add(forceCheck);
        JCheckBox reloadCheck = new JCheckBox("-r|--reload  Send reload request - Pragma: no-cache.");
        reloadCheck.setSelected(true);
        pLeft.add(reloadCheck);

        String[] protocols=new String[]{"-9|--http09  Use HTTP/0.9 style requests.","-1|--http10  Use HTTP/1.0 protocol.","-2|--http11  Use HTTP/1.1 protocol."};
        JComboBox protocolBox = new JComboBox(protocols);
        protocolBox.setBounds(50, 50, 80, 30);
        pLeft.add(protocolBox);

        JRadioButton getReq = new JRadioButton("--get  Use GET request method.");
        getReq.setSelected(true);
        JRadioButton headReq = new JRadioButton("--head  Use HEAD request method.");
        JRadioButton optionsReq = new JRadioButton("--options  Use OPTIONS request method.");
        JRadioButton traceReq = new JRadioButton("--trace  Use TRACE request method.");
        ButtonGroup reqGroup=new ButtonGroup();
        reqGroup.add(getReq);
        reqGroup.add(headReq);
        reqGroup.add(optionsReq);
        reqGroup.add(traceReq);

        JPanel reqPane = new JPanel();
        reqPane.add(getReq);
        reqPane.add(headReq);
        reqPane.add(optionsReq);
        reqPane.add(traceReq);
        pLeft.add(reqPane);


        JPanel pRight = new JPanel();
        pRight.setBounds(10,150,300,60);
        JButton runButton = new JButton("RUN");
        pRight.add(runButton);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setValue(50);
        progressBar.setStringPainted(true);
        pRight.add(progressBar);

        JSplitPane sp=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,pLeft,pRight);
        sp.setDividerLocation(400);

        f.setContentPane(sp);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
    }
}
