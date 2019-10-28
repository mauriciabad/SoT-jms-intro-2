import com.google.gson.Gson;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Receiver {

  private Session session;
  private Destination sendDestination;
  private Connection connection;
  private MessageConsumer consumer;

  private List<Message> messages = new ArrayList<Message>();
  private List<String> messagesTitles = new ArrayList<String>();

  private Runnable onMessageChange;

  public Receiver(Runnable onMessageChange) {
    this.onMessageChange = onMessageChange;
    init();

    try {
      consumer.setMessageListener(new MessageListener() {
        @Override
        public void onMessage(Message msg) {
          messages.add(msg);
          onMessageChange.run();
        }
      });
      connection.start(); // this is needed to start receiving messages
    } catch (JMSException e) {
      System.out.println("Error creating message listener");
      e.printStackTrace();
    }
  }

  private void init() {
    try {
      connection = new ActiveMQConnectionFactory("tcp://localhost:61616").createConnection();
      session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);;
      sendDestination = session.createQueue("ChatGroup1");
      consumer = session.createConsumer(sendDestination);
    } catch (JMSException e) {
      System.out.println("MQ not running in tcp://localhost:61616");
      e.printStackTrace();
    }
  }

  public static void displayMessage(TextMessage msg) {
    try {
      System.out.println("JMSMessageID = " + msg.getJMSMessageID());
      System.out.println("  JMSDestination = " + msg.getJMSDestination());
      System.out.println("  Text = " + msg.getText());
    } catch (JMSException e) {
      System.out.println("sent: " + msg);
    }
  }

  public void replyMessage(int index, String body) throws JMSException {
    Message msg = messages.remove(index);

    onMessageChange.run();
  }

  public List getMessages() {
    return messages;
  }

  public List getMessagesTitles() {
    return messages.stream().map(msg -> {
      try {
        return ((TextMessage) msg).getText();
      } catch (JMSException e) {
        return "Unreadable message";
      }
    }).collect(Collectors.toList());
  }
}