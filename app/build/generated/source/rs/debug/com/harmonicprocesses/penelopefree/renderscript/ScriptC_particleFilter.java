/*
 * Copyright (C) 2011-2014 The Android Open Source Project
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

/*
 * This file is auto-generated. DO NOT MODIFY!
 * The source Renderscript file: /Users/izzy/AndroidstudioProjects/Penelope/app/src/main/rs/particleFilter.rs
 */

package com.harmonicprocesses.penelopefree.renderscript;

import android.renderscript.*;
import android.content.res.Resources;

/**
 * @hide
 */
public class ScriptC_particleFilter extends ScriptC {
    private static final String __rs_resource_name = "particlefilter";
    // Constructor
    public  ScriptC_particleFilter(RenderScript rs) {
        this(rs,
             rs.getApplicationContext().getResources(),
             rs.getApplicationContext().getResources().getIdentifier(
                 __rs_resource_name, "raw",
                 rs.getApplicationContext().getPackageName()));
    }

    public  ScriptC_particleFilter(RenderScript rs, Resources resources, int id) {
        super(rs, resources, id);
        __ScriptField_particle = ScriptField_particle.createElement(rs);
    }

    private Element __ScriptField_particle;
    private FieldPacker __rs_fp_ScriptField_particle;
    private final static int mExportVarIdx_vin = 0;
    private ScriptField_particle.Item mExportVar_vin;
    public synchronized void set_vin(ScriptField_particle.Item v) {
        mExportVar_vin = v;
        FieldPacker fp = new FieldPacker(32);
        fp.addF32(v.x);
        fp.addF32(v.y);
        fp.addF32(v.launchAngle);
        fp.addF32(v.theta);
        fp.addF32(v.furlong);
        fp.addF32(v.distance2edge);
        fp.addF32(v.delta);
        fp.addF32(v.amplitude);
        int []__dimArr = new int[1];
        __dimArr[0] = 1;
        setVar(mExportVarIdx_vin, fp, __ScriptField_particle, __dimArr);
    }

    public ScriptField_particle.Item get_vin() {
        return mExportVar_vin;
    }

    public Script.FieldID getFieldID_vin() {
        return createFieldID(mExportVarIdx_vin, null);
    }

    private final static int mExportForEachIdx_root = 0;
    public Script.KernelID getKernelID_root() {
        return createKernelID(mExportForEachIdx_root, 3, null, null);
    }

    public void forEach_root(Allocation ain, Allocation aout) {
        forEach_root(ain, aout, null);
    }

    public void forEach_root(Allocation ain, Allocation aout, Script.LaunchOptions sc) {
        // check ain
        if (!ain.getType().getElement().isCompatible(__ScriptField_particle)) {
            throw new RSRuntimeException("Type mismatch with ScriptField_particle!");
        }
        // check aout
        if (!aout.getType().getElement().isCompatible(__ScriptField_particle)) {
            throw new RSRuntimeException("Type mismatch with ScriptField_particle!");
        }
        Type t0, t1;        // Verify dimensions
        t0 = ain.getType();
        t1 = aout.getType();
        if ((t0.getCount() != t1.getCount()) ||
            (t0.getX() != t1.getX()) ||
            (t0.getY() != t1.getY()) ||
            (t0.getZ() != t1.getZ()) ||
            (t0.hasFaces()   != t1.hasFaces()) ||
            (t0.hasMipmaps() != t1.hasMipmaps())) {
            throw new RSRuntimeException("Dimension mismatch between parameters ain and aout!");
        }

        forEach(mExportForEachIdx_root, ain, aout, null, sc);
    }

    private final static int mExportFuncIdx_getNextPosition = 0;
    public void invoke_getNextPosition() {
        invoke(mExportFuncIdx_getNextPosition);
    }

    private final static int mExportFuncIdx_getTurnPosition = 1;
    public void invoke_getTurnPosition(float distancePastEdge) {
        FieldPacker getTurnPosition_fp = new FieldPacker(4);
        getTurnPosition_fp.addF32(distancePastEdge);
        invoke(mExportFuncIdx_getTurnPosition, getTurnPosition_fp);
    }

}

