package com.humolix.volur.security.flow.domain;

import com.humolix.volur.framework.common.exception.BusinessException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
public enum FlowNames {

    VLIF_WEB_LOGIN("VlifWebLogin", FlowEntry.WEB_SERVICE_LOGIN),
    VLIF_WEB_SIGNUP("VlifWebSignUp", FlowEntry.WEB_SIGN_UP);

    static {
        for (FlowNames fn : values()) {
            for (FlowEntry fe : fn.entries) {
                fe.flowNames = fn;
            }
        }
    }

    private final String flowName;
    private final Set<FlowEntry> entries;

    FlowNames(String flowName, FlowEntry... entries) {
        this.flowName = flowName;
        this.entries = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(entries)));
    }

    public FlowEntry getDefaultEntry() {
        return entries.iterator().next();
    }

    public boolean hasEntry(String entryName) {
        if (entryName == null || entryName.isBlank()) {
            return false;
        }
        String trimmed = entryName.trim();
        return entries.stream().anyMatch(e -> e.getEntry().equalsIgnoreCase(trimmed) || e.name().equalsIgnoreCase(trimmed));
    }

    public boolean hasEntry(FlowEntry entry) {
        return entry != null && entries.contains(entry);
    }

    public FlowEntry findEntry(String entryName) {
        if (entryName == null || entryName.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "流程入口不能为空");
        }
        String trimmed = entryName.trim();
        for (FlowEntry fe : entries) {
            if (fe.getEntry().equalsIgnoreCase(trimmed) || fe.name().equalsIgnoreCase(trimmed)) {
                return fe;
            }
        }
        throw new BusinessException(HttpStatus.BAD_REQUEST, "流程入口 '" + entryName + "' 不属于流程 '" + flowName + "'");
    }

    public static FlowNames from(String flowName) {
        if (flowName == null || flowName.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "流程名称不能为空");
        }
        String trimmed = flowName.trim();
        for (FlowNames fn : values()) {
            if (fn.flowName.equalsIgnoreCase(trimmed) || fn.name().equalsIgnoreCase(trimmed)) {
                return fn;
            }
        }
        throw new BusinessException(HttpStatus.BAD_REQUEST, "非法或未知的流程名称: " + flowName);
    }

    public static boolean isValidFlowName(String flowName) {
        if (flowName == null || flowName.isBlank()) {
            return false;
        }
        String trimmed = flowName.trim();
        for (FlowNames fn : values()) {
            if (fn.flowName.equalsIgnoreCase(trimmed) || fn.name().equalsIgnoreCase(trimmed)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isValid(String flowName, String flowEntry) {
        if (flowName == null || flowName.isBlank() || flowEntry == null || flowEntry.isBlank()) {
            return false;
        }
        try {
            FlowNames fn = from(flowName);
            return fn.hasEntry(flowEntry);
        } catch (BusinessException e) {
            return false;
        }
    }

    public static FlowNames validate(String flowName, String flowEntry) {
        FlowNames fn = from(flowName);
        fn.findEntry(flowEntry);
        return fn;
    }

    public static FlowNames fromEntry(FlowEntry entry) {
        if (entry == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "流程入口不能为空");
        }
        if (entry.getFlowNames() != null) {
            return entry.getFlowNames();
        }
        for (FlowNames fn : values()) {
            if (fn.hasEntry(entry)) {
                return fn;
            }
        }
        throw new BusinessException(HttpStatus.BAD_REQUEST, "未找到该流程入口所属的流程: " + entry.getEntry());
    }

    @Getter
    public enum FlowEntry {

        WEB_SERVICE_LOGIN("WebServiceLogin"),
        WEB_SIGN_UP("WebSignUp");

        private final String entry;
        private FlowNames flowNames;

        FlowEntry(String entry) {
            this.entry = entry;
        }

        public static FlowEntry from(String entry) {
            if (entry == null || entry.isBlank()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "流程入口不能为空");
            }
            String trimmed = entry.trim();
            for (FlowEntry fe : values()) {
                if (fe.entry.equalsIgnoreCase(trimmed) || fe.name().equalsIgnoreCase(trimmed)) {
                    return fe;
                }
            }
            throw new BusinessException(HttpStatus.BAD_REQUEST, "非法或未知的流程入口: " + entry);
        }

        public static boolean isValid(String entry) {
            if (entry == null || entry.isBlank()) {
                return false;
            }
            String trimmed = entry.trim();
            for (FlowEntry fe : values()) {
                if (fe.entry.equalsIgnoreCase(trimmed) || fe.name().equalsIgnoreCase(trimmed)) {
                    return true;
                }
            }
            return false;
        }
    }
}
