package net.vinote.smart.socket.protocol.p2p.server;

import java.util.Properties;

import net.vinote.smart.socket.extension.cluster.ClusterMessageEntry;
import net.vinote.smart.socket.lang.QuicklyConfig;
import net.vinote.smart.socket.lang.StringUtils;
import net.vinote.smart.socket.protocol.DataEntry;
import net.vinote.smart.socket.protocol.P2PSession;
import net.vinote.smart.socket.protocol.p2p.message.BaseMessage;
import net.vinote.smart.socket.protocol.p2p.message.ClusterMessageReq;
import net.vinote.smart.socket.protocol.p2p.message.InvalidMessageReq;
import net.vinote.smart.socket.protocol.p2p.processor.InvalidMessageProcessor;
import net.vinote.smart.socket.service.process.AbstractProtocolDisruptorProcessor;
import net.vinote.smart.socket.service.process.AbstractServiceMessageProcessor;
import net.vinote.smart.socket.service.session.Session;
import net.vinote.smart.socket.service.session.SessionManager;
import net.vinote.smart.socket.transport.TransportSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务器消息处理器,由服务器启动时构造
 *
 * @author Seer
 *
 */
public class P2PServerDisruptorProcessor extends AbstractProtocolDisruptorProcessor {
	private Logger logger = LoggerFactory.getLogger(P2PServerDisruptorProcessor.class);

	public ClusterMessageEntry generateClusterMessage(DataEntry data) {
		ClusterMessageReq entry = new ClusterMessageReq();
		entry.setServiceData(data);
		return entry;
	}

	@Override
	public void init(QuicklyConfig config) throws Exception {
		super.init(config);
		// 启动线程池处理消息
		Properties properties = new Properties();
		properties.put(InvalidMessageReq.class.getName(), InvalidMessageProcessor.class.getName());
		config.getServiceMessageFactory().loadFromProperties(properties);
	}

	public <T> void process(T t) {
		ProcessUnit unit = (ProcessUnit) t;
		Session session = SessionManager.getInstance().getSession(unit.sessionId);
		try {
			if (session == null || session.isInvalid()) {
				logger.info("Session is invalid,lose message" + StringUtils.toHexString(unit.msg.getData()));
				return;
			}
			BaseMessage baseMsg = (BaseMessage) unit.msg;
			// 解密消息
			if (baseMsg.getHead().isSecure()) {
				baseMsg.getHead().setSecretKey(session.getAttribute(StringUtils.SECRET_KEY, byte[].class));
				baseMsg.decode();
			}
			session.refreshAccessedTime();
			AbstractServiceMessageProcessor processor = getQuicklyConfig().getServiceMessageFactory().getProcessor(
				unit.msg.getClass());
			processor.processor(session, unit.msg);
		} catch (Exception e) {
			logger.warn("", e);
		}

	}

	@Override
	public void shutdown() {
		super.shutdown();
		getQuicklyConfig().getServiceMessageFactory().destory();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * net.vinote.smart.socket.service.process.ProtocolDataProcessor#getSession
	 * (net.vinote.smart.socket.transport.TransportSession)
	 */
	@Override
	public Session getSession(TransportSession tsession) {
		Session session = SessionManager.getInstance().getSession(tsession.getSessionID());
		if (session == null) {
			session = new P2PSession(tsession);
			SessionManager.getInstance().registSession(session);
		}
		session.refreshAccessedTime();
		return session;
	}
}
