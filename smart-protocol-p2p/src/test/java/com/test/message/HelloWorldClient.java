package com.test.message;

import java.util.Properties;

import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.protocol.P2PProtocolFactory;
import net.vinote.smart.socket.protocol.p2p.client.P2PClientMessageProcessor;
import net.vinote.smart.socket.protocol.p2p.message.P2pServiceMessageFactory;
import net.vinote.smart.socket.service.factory.ServiceMessageFactory;
import net.vinote.smart.socket.transport.nio.NioQuickClient;

public class HelloWorldClient {
	public static void main(String[] args) throws Exception {

		QuicklyConfig config = new QuicklyConfig(false);

		P2PProtocolFactory factory = new P2PProtocolFactory();
		config.setProtocolFactory(factory);
		P2PClientMessageProcessor processor = new P2PClientMessageProcessor();
		config.setProcessor(processor);
		config.setHost("127.0.0.1");
		config.setTimeout(1000);

		Properties properties = new Properties();
		properties.put(HelloWorldResp.class.getName(), "");
		ServiceMessageFactory messageFactory = new P2pServiceMessageFactory();
		messageFactory.loadFromProperties(properties);
		config.setServiceMessageFactory(messageFactory);

		NioQuickClient client = new NioQuickClient(config);
		client.start();

		int num = 10;
		while (num-- > 0) {
			HelloWorldReq req = new HelloWorldReq();
			req.setName("seer" + num);
			req.setAge(num);
			req.setMale(num % 2 == 0);
			processor.getSession().sendWithResponse(req);
		}
		client.shutdown();
	}
}
