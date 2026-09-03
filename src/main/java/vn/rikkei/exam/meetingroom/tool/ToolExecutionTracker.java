package vn.rikkei.exam.meetingroom.tool;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class ToolExecutionTracker {

    private final ThreadLocal<Set<String>> tools = ThreadLocal.withInitial(LinkedHashSet::new);

    public void reset() {
        tools.get().clear();
    }

    public void record(String toolName) {
        tools.get().add(toolName);
    }

    public List<String> snapshot() {
        return new ArrayList<>(tools.get());
    }

    public void clear() {
        tools.remove();
    }
}
