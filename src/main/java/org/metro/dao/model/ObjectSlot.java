package org.metro.dao.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Date;

@Data
public class ObjectSlot {

    private Integer objId;

    private String objName;

    @JsonIgnore
    private String objMime;

    private Date createDate;

    private Date updateDate;

    @JsonIgnore
    public String getPath() {
        if (objId == null || objMime == null) {
            return null;
        }
        int typeIdx = objMime.indexOf('/'), extIdx = objMime.lastIndexOf('/');
        String type = objMime.substring(0, typeIdx);
        String extension = typeIdx == extIdx ? "" : '.' + objMime.substring(extIdx+1);
        return StringUtils.join(File.separatorChar, type, File.separatorChar, prettyID(objId), extension);
    }

    public String getContentType() {
        if (objMime == null) {
            return null;
        }
        int typeIdx = objMime.indexOf('/'), extIdx = objMime.lastIndexOf('/');
        return typeIdx == extIdx ? objMime: objMime.substring(0, extIdx);
    }

    @JsonIgnore
    public String getFileName() {
        if (objName == null|| objMime == null) {
            return null;
        }
        int typeIdx = objMime.indexOf('/'), extIdx = objMime.lastIndexOf('/');
        return typeIdx == extIdx ? objName : objName + '.' + objMime.substring(extIdx+1);
    }

    private static String prettyID(int id) {
        String s36 = Integer.toUnsignedString(id, 36);
        StringBuilder code = new StringBuilder();
        int pad = 7 - s36.length();
        while (pad-- > 0) code.append('0');
        for (int i=0; i<s36.length(); i++) {
            code.append(Character.toUpperCase(s36.charAt(i)));
        }
        return code.toString();
    }

}