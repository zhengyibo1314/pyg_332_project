package cn.itcast.core.listener;

import cn.itcast.core.service.search.ItemSearchService;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
//自定义监听器 :将商品信息保存到索引库中
    public class ItemSearchListener implements MessageListener {
    @Resource
    private ItemSearchService itemSearchService;
   //获取容器中的消息
    @Override
    public void onMessage(Message message) {
        try {
          //先取出消息
          ActiveMQTextMessage activeMQTextMessage= (ActiveMQTextMessage) message;
          String id = activeMQTextMessage.getText();
            System.out.println("消费者search 获取到id为="+id);
            //消费者
            itemSearchService.updateItemToSolr(Long.parseLong(id));
        } catch (JMSException e) {
            e.printStackTrace();
        }

    }
}
