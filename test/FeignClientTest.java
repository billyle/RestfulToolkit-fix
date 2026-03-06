package test;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;

@FeignClient(name = "yl-jms-app-api", path = "/appapi")
public interface AppApiFeignClient {

    /**
     * 保存app事件日志
     */
    @PostMapping("/event/log/save")
    Result saveEventLog(@RequestBody List<TabAppEventsLog> record);
    
    // 测试类
    class Result {
        private boolean success;
        private String message;
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    class TabAppEventsLog {
        private String eventId;
        private String appName;
        
        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }
        public String getAppName() { return appName; }
        public void setAppName(String appName) { this.appName = appName; }
    }
}