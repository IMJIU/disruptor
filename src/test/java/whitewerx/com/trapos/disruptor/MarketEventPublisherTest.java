package whitewerx.com.trapos.disruptor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SingleThreadedClaimStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;

import whitewerx.com.trapos.gateway.TextMessageSubscriber;
import whitewerx.com.trapos.test.LongEvent;
import whitewerx.com.trapos.test.LongEventFactory;
import whitewerx.com.trapos.test.LongEventHandler;

@RunWith(JMock.class)
public class MarketEventPublisherTest {

	Mockery context = new Mockery() {
		{
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};

	@Test
	public void publishEvent() {
		final String delimitedMessage = "T|B|5.1t|R|EURUSD|1.3124";
		final MarketEvent emptyEvent = context.mock(MarketEvent.class);
		@SuppressWarnings("unchecked")
		final RingBufferAdapter<MarketEvent> ringBuffer = (RingBufferAdapter<MarketEvent>) context.mock(RingBufferAdapter.class);

		TextMessageSubscriber publisher = new MarketEventPublisher(ringBuffer);
		context.checking(new Expectations() {
			{
				final long SEQUENCE = 1;

				oneOf(ringBuffer).next();
				will(returnValue(SEQUENCE));

				oneOf(ringBuffer).get(SEQUENCE);
				will(returnValue(emptyEvent));

				oneOf(emptyEvent).setMessage(delimitedMessage);

				oneOf(ringBuffer).publish(SEQUENCE);
			}
		});

		publisher.accept(delimitedMessage);
	}

	@Test
	public void publishEvent2() {
		final String delimitedMessage = "T|B|5.1t|R|EURUSD|1.3124";
		final MarketEvent emptyEvent = context.mock(MarketEvent.class);
		@SuppressWarnings("unchecked")
		// final RingBufferAdapter<MarketEvent> ringBuffer = new
		// RingBufferAdapter<>(new RingBuffer<>(eventFactory, bufferSize))
		RingBuffer<MarketEvent> ringBuffer = new RingBuffer<MarketEvent>(MarketEvent.FACTORY, new SingleThreadedClaimStrategy(16), new BlockingWaitStrategy());
		MarketEventPublisher eventPublisher = new MarketEventPublisher(new RingBufferAdapter<MarketEvent>(ringBuffer));
		ringBuffer.next();
		ringBuffer.get(1);
		ringBuffer.setGatingSequences();
		ringBuffer.publish(1);
		eventPublisher.accept(delimitedMessage);
	}

	@Test
	public void publishEvent3() {
		EventFactory<LongEvent> eventFactory = new LongEventFactory();
		ExecutorService executor = Executors.newSingleThreadExecutor();
		int ringBufferSize = 1024 * 1024; // RingBuffer 大小，必须是 2 的 N 次方；

		Disruptor<LongEvent> disruptor = new Disruptor<LongEvent>(eventFactory, ringBufferSize, executor);

		EventHandler<LongEvent> eventHandler = new LongEventHandler();
		disruptor.handleEventsWith(eventHandler);

		disruptor.start();
		// 发布事件；
		RingBuffer<LongEvent> ringBuffer = disruptor.getRingBuffer();
		long sequence = ringBuffer.next();// 请求下一个事件序号；

		LongEvent event = ringBuffer.get(sequence);// 获取该序号对应的事件对象；
		long data = 1;// 获取要通过事件传递的业务数据；
		event.set(data);

		ringBuffer.publish(sequence);// 发布事件；

	}

}
