/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.autofill.service;

import android.app.assist.AssistStructure;
import android.app.assist.AssistStructure.ViewNode;
import android.app.assist.AssistStructure.WindowNode;
import android.os.Bundle;
import android.service.autofill.FillContext;
import android.service.autofill.SaveInfo;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewStructure.HtmlInfo;
import android.view.autofill.AutofillValue;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public final class Util {

    public static final String TAG = "AutofillSample";
    public static final boolean DEBUG = true;
    public static final boolean VERBOSE = false;
    public static final String EXTRA_DATASET_NAME = "dataset_name";
    public static final String EXTRA_FOR_RESPONSE = "for_response";
    public static final NodeFilter AUTOFILL_ID_FILTER = (node, id) -> {
        return id.equals(node.getAutofillId());
    };

    private static void bundleToString(StringBuilder builder, Bundle data) {
        final Set<String> keySet = data.keySet();
        builder.append("[Bundle with ").append(keySet.size()).append(" keys:");
        for (String key : keySet) {
            builder.append(' ').append(key).append('=');
            Object value = data.get(key);
            if ((value instanceof Bundle)) {
                bundleToString(builder, (Bundle) value);
            } else {
                builder.append((value instanceof Object[])
                        ? Arrays.toString((Object[]) value) : value);
            }
        }
        builder.append(']');
    }

    public static String bundleToString(Bundle data) {
        if (data == null) {
            return "N/A";
        }
        final StringBuilder builder = new StringBuilder();
        bundleToString(builder, data);
        return builder.toString();
    }

    public static String getTypeAsString(int type) {
        switch (type) {
            case View.AUTOFILL_TYPE_TEXT:
                return "TYPE_TEXT";
            case View.AUTOFILL_TYPE_LIST:
                return "TYPE_LIST";
            case View.AUTOFILL_TYPE_NONE:
                return "TYPE_NONE";
            case View.AUTOFILL_TYPE_TOGGLE:
                return "TYPE_TOGGLE";
            case View.AUTOFILL_TYPE_DATE:
                return "TYPE_DATE";
        }
        return "UNKNOWN_TYPE";
    }

    private static String getAutofillValueAndTypeAsString(AutofillValue value) {
        if (value == null) return "null";

        StringBuilder builder = new StringBuilder(value.toString()).append('(');
        if (value.isText()) {
            builder.append("isText");
        } else if (value.isDate()) {
            builder.append("isDate");
        } else if (value.isToggle()) {
            builder.append("isToggle");
        } else if (value.isList()) {
            builder.append("isList");
        }
        return builder.append(')').toString();
    }

    public static void dumpStructure(AssistStructure structure) {
        int nodeCount = structure.getWindowNodeCount();
        Log.v(TAG, "dumpStructure(): component=" + structure.getActivityComponent()
                + " numberNodes=" + nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            Log.v(TAG, "node #" + i);
            WindowNode node = structure.getWindowNodeAt(i);
            dumpNode("  ", node.getRootViewNode());
        }
    }

    private static void dumpNode(String prefix, ViewNode node) {
        StringBuilder builder = new StringBuilder();
        builder.append(prefix)
                .append("autoFillId: ").append(node.getAutofillId())
                .append("\tidEntry: ").append(node.getIdEntry())
                .append("\tid: ").append(node.getId())
                .append("\tclassName: ").append(node.getClassName())
                .append('\n');

        builder.append(prefix)
                .append("focused: ").append(node.isFocused())
                .append("\tvisibility").append(node.getVisibility())
                .append("\tchecked: ").append(node.isChecked())
                .append("\twebDomain: ").append(node.getWebDomain())
                .append("\thint: ").append(node.getHint())
                .append('\n');

        HtmlInfo htmlInfo = node.getHtmlInfo();

        if (htmlInfo != null) {
            builder.append(prefix)
                    .append("HTML TAG: ").append(htmlInfo.getTag())
                    .append(" attrs: ").append(htmlInfo.getAttributes())
                    .append('\n');
        }

        String[] afHints = node.getAutofillHints();
        CharSequence[] options = node.getAutofillOptions();
        builder.append(prefix).append("afType: ").append(getTypeAsString(node.getAutofillType()))
                .append("\tafValue:")
                .append(getAutofillValueAndTypeAsString(node.getAutofillValue()))
                .append("\tafOptions:").append(options == null ? "N/A" : Arrays.toString(options))
                .append("\tafHints: ").append(afHints == null ? "N/A" : Arrays.toString(afHints))
                .append("\tinputType:").append(node.getInputType())
                .append('\n');

        int numberChildren = node.getChildCount();
        builder.append(prefix).append("# children: ").append(numberChildren)
                .append("\ttext: ").append(node.getText())
                .append('\n');

        Log.v(TAG, builder.toString());
        final String prefix2 = prefix + "  ";
        for (int i = 0; i < numberChildren; i++) {
            Log.v(TAG, prefix + "child #" + i);
            dumpNode(prefix2, node.getChildAt(i));
        }
    }

    public static String getSaveTypeAsString(int type) {
        List<String> types = new ArrayList<>();
        if ((type & SaveInfo.SAVE_DATA_TYPE_ADDRESS) != 0) {
            types.add("ADDRESS");
        }
        if ((type & SaveInfo.SAVE_DATA_TYPE_CREDIT_CARD) != 0) {
            types.add("CREDIT_CARD");
        }
        if ((type & SaveInfo.SAVE_DATA_TYPE_EMAIL_ADDRESS) != 0) {
            types.add("EMAIL_ADDRESS");
        }
        if ((type & SaveInfo.SAVE_DATA_TYPE_USERNAME) != 0) {
            types.add("USERNAME");
        }
        if ((type & SaveInfo.SAVE_DATA_TYPE_PASSWORD) != 0) {
            types.add("PASSWORD");
        }
        if (types.isEmpty()) {
            return "UNKNOWN(" + type + ")";
        }
        return Joiner.on('|').join(types);
    }

    /**
     * Gets a node if it matches the filter criteria for the given id.
     */
    public static ViewNode findNodeByFilter(@NonNull List<FillContext> contexts, @NonNull Object id,
            @NonNull NodeFilter filter) {
        for (FillContext context : contexts) {
            ViewNode node = findNodeByFilter(context.getStructure(), id, filter);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    /**
     * Gets a node if it matches the filter criteria for the given id.
     */
    public static ViewNode findNodeByFilter(@NonNull AssistStructure structure, @NonNull Object id,
            @NonNull NodeFilter filter) {
        Log.v(TAG, "Parsing request for activity " + structure.getActivityComponent());
        final int nodes = structure.getWindowNodeCount();
        for (int i = 0; i < nodes; i++) {
            final WindowNode windowNode = structure.getWindowNodeAt(i);
            final ViewNode rootNode = windowNode.getRootViewNode();
            final ViewNode node = findNodeByFilter(rootNode, id, filter);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    /**
     * Gets a node if it matches the filter criteria for the given id.
     */
    public static ViewNode findNodeByFilter(@NonNull ViewNode node, @NonNull Object id,
            @NonNull NodeFilter filter) {
        if (filter.matches(node, id)) {
            return node;
        }
        final int childrenSize = node.getChildCount();
        if (childrenSize > 0) {
            for (int i = 0; i < childrenSize; i++) {
                final ViewNode found = findNodeByFilter(node.getChildAt(i), id, filter);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Helper interface used to filter Assist nodes.
     */
    public interface NodeFilter {
        /**
         * Returns whether the node passes the filter for such given id.
         */
        boolean matches(ViewNode node, Object id);
    }
}