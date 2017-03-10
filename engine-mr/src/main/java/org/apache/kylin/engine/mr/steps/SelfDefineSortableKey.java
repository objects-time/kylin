/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.apache.kylin.engine.mr.steps;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.kylin.metadata.datatype.DataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelfDefineSortableKey implements WritableComparable<SelfDefineSortableKey> {

    private static final Logger logger = LoggerFactory.getLogger(SelfDefineSortableKey.class);

    public enum TypeFlag {
        NONE_NUMERIC_TYPE, INTEGER_FAMILY_TYPE, DOUBLE_FAMILY_TYPE
    }

    private byte typeId; //non-numeric(0000 0000) int(0000 0001) other numberic(0000 0010)

    private Text rawKey;

    private Object keyInObj;

    public SelfDefineSortableKey() {
    }

    public void init(Text key, byte typeId) {
        this.typeId = typeId;
        this.rawKey = key;
        if (isNumberFamily()) {
            String valueStr = new String(key.getBytes(), 1, key.getLength() - 1);
            if (isIntegerFamily()) {
                this.keyInObj = Integer.parseInt(valueStr);
            } else {
                this.keyInObj = Double.parseDouble(valueStr);
            }
        } else {
            this.keyInObj = key;
        }
    }

    public void init(Text key, DataType type) {
        byte typeIdByDatatype = getTypeIdByDatatype(type);
        logger.info("type is {}, byte is {} ", type, typeIdByDatatype);
        init(key, typeIdByDatatype);
    }

    @Override
    public int compareTo(SelfDefineSortableKey o) {
        if (this.typeId != o.typeId)
            throw new IllegalStateException("Error. Incompatible types");
        if (!isNumberFamily()) {
            return ((Text) this.keyInObj).compareTo(((Text) o.keyInObj));
        } else {
            if (isIntegerFamily()) {
                return Integer.compare((Integer) this.keyInObj, (Integer) o.keyInObj);
            } else {
                return Double.compare((Double) this.keyInObj, (Double) o.keyInObj);
            }
        }
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeByte(typeId);
        rawKey.write(dataOutput);
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        this.typeId = dataInput.readByte();
        Text inputKey = new Text();
        inputKey.readFields(dataInput);
        init(inputKey, typeId);
    }

    public short getTypeId() {
        return typeId;
    }

    public Text getText() {
        return rawKey;
    }

    public boolean isNumberFamily() {
        if (typeId == TypeFlag.NONE_NUMERIC_TYPE.ordinal())
            return false;
        return true;
    }

    public boolean isIntegerFamily() {
        return (typeId == TypeFlag.INTEGER_FAMILY_TYPE.ordinal());
    }

    public byte getTypeIdByDatatype(DataType type) {
        if (!type.isNumberFamily()) {
            return (byte) TypeFlag.NONE_NUMERIC_TYPE.ordinal();
        } else if (type.isIntegerFamily()) {
            return (byte) TypeFlag.INTEGER_FAMILY_TYPE.ordinal();
        } else {
            return (byte) TypeFlag.DOUBLE_FAMILY_TYPE.ordinal();
        }
    }

    public void setTypeId(byte typeId) {
        this.typeId = typeId;
    }

}
