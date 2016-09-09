package whitewerx.com.trapos.test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

public class T01 {
	static class Translator implements EventTranslatorOneArg<LongEvent, Long>{
	    @Override
	    public void translateTo(LongEvent event, long sequence, Long data) {
	        event.set(data);
	    }    
	}
	public static Translator TRANSLATOR = new Translator();
    
	public static void publishEvent2(Disruptor<LongEvent> disruptor) {
	    // 发布事件；
	    RingBuffer<LongEvent> ringBuffer = disruptor.getRingBuffer();
	    long data = getEventData();//获取要通过事件传递的业务数据；
	    ringBuffer.publishEvent(TRANSLATOR, data);
	}
	
	public static void main(String[] args) {
		EventFactory<LongEvent> eventFactory = new LongEventFactory();
		ExecutorService executor = Executors.newSingleThreadExecutor();
		int ringBufferSize = 1024 * 1024; // RingBuffer 大小，必须是 2 的 N 次方；

		Disruptor<LongEvent> disruptor = new Disruptor<LongEvent>(eventFactory, ringBufferSize, executor);

		EventHandler<LongEvent> eventHandler = new LongEventHandler();
		disruptor.handleEventsWith(eventHandler);

		disruptor.start();
		// 发布事件；
		RingBuffer<LongEvent> ringBuffer = disruptor.getRingBuffer();
		long sequence = ringBuffer.next();//请求下一个事件序号；
		    
		try {
		    LongEvent event = ringBuffer.get(sequence);//获取该序号对应的事件对象；
		    long data = getEventData();//获取要通过事件传递的业务数据；
		    event.set(data);
		} finally{
		    ringBuffer.publish(sequence);//发布事件；
		}
	}

}

    
