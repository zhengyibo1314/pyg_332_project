package cn.itcast.core.listener;

import cn.itcast.core.service.search.ItemSearchService;
import com.sun.xml.internal.bind.v2.model.core.ID;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
//从索引库中删除索引的数据
public class ItemDeleteListener implements MessageListener {
    @Resource
    private ItemSearchService itemSearchService;

    @Override
    public void onMessage(Message message) {

        try {
            ActiveMQTextMessage activeMQTextMessage=(ActiveMQTextMessage) message;
            String id = activeMQTextMessage.getText();
            System.out.println("消费者获取id"+id);
            //消费消息
            itemSearchService.deleteItemFromSolr(Long.parseLong(id));
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
