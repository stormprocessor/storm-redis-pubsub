package yieldbot.storm.spout;

import static backtype.storm.utils.Utils.tuple;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.utils.Utils;

public class RedisPubSubSpout extends BaseRichSpout {
	
	static final long serialVersionUID = 737015318988609460L;
	static Logger LOG = Logger.getLogger(RedisPubSubSpout.class);
	
	SpoutOutputCollector _collector;
	final String host;
	final int port;
	final String pattern;
	LinkedBlockingQueue<String> queue;
	
	public RedisPubSubSpout(String host, int port, String pattern) {
		this.host = host;
		this.port = port;
		this.pattern = pattern;
	}
	
	class ListenerThread extends Thread {
		LinkedBlockingQueue<String> queue;
		Jedis jedis;
		String pattern;
		
		public ListenerThread(LinkedBlockingQueue<String> queue, Jedis jedis, String pattern) {
			this.queue = queue;
			this.jedis = jedis;
			this.pattern = pattern;
		}
		
		public void run() {
			
			JedisPubSub listener = new JedisPubSub() {

				@Override
				public void onMessage(String channel, String message) {
					queue.offer(message);
				}

				@Override
				public void onPMessage(String pattern, String channel, String message) {
					queue.offer(message);
				}

				@Override
				public void onPSubscribe(String channel, int subscribedChannels) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onPUnsubscribe(String channel, int subscribedChannels) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onSubscribe(String channel, int subscribedChannels) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onUnsubscribe(String channel, int subscribedChannels) {
					// TODO Auto-generated method stub
					
				}
			};
			jedis.psubscribe(listener, pattern);
		}
	};
	
	public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
		_collector = collector;
		queue = new LinkedBlockingQueue<String>(1000);
		Jedis jedis = new Jedis(host,port);
		
		ListenerThread listener = new ListenerThread(queue,jedis,pattern);
		listener.start();
		
	}

	public void close() {}

	public void nextTuple() {
		String ret = queue.poll();
        	if(ret==null) {
            		Utils.sleep(50);
        	} else {
            		_collector.emit(tuple(ret));            
        	}
	}

	public void ack(Object msgId) {
		// TODO Auto-generated method stub

	}

	public void fail(Object msgId) {
		// TODO Auto-generated method stub

	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("message"));
	}

	public boolean isDistributed() {
		return false;
	}
}
