package labs.rsreu.exchanges;

import com.lmax.disruptor.EventHandler;

public class ResponseHandler implements EventHandler<ResponseEvent> {
    @Override
    public void onEvent(ResponseEvent event, long sequence, boolean endOfBatch) {
        // Обработка ответа и передача клиенту
        handleResponse(event);
    }

    private void handleResponse(ResponseEvent event) {
        // Логика обработки ответа
    }
}

