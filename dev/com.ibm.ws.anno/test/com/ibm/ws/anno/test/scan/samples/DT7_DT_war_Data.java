/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.anno.test.scan.samples;

import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Direct_WAR;
import com.ibm.ws.anno.test.utils.TestLocalization;
import com.ibm.wsspi.anno.classsource.ClassSource_Factory;

public class DT7_DT_war_Data {
    public static final String EAR_NAME = "dt7.ear";
    public static final String EAR_SIMPLE_NAME = "dt7";
    
    public static final String WAR_NAME = "daytrader-ee7-web.war";
    public static final String WAR_SIMPLE_NAME = "daytrader";

    public static ClassSource_Specification_Direct_WAR createClassSourceSpecification(ClassSource_Factory factory) {

        ClassSource_Specification_Direct_WAR warSpecification =
            factory.newWARDirectSpecification(EAR_SIMPLE_NAME, WAR_SIMPLE_NAME);

        String earPath = TestLocalization.putIntoData(EAR_NAME) + '/';

        String warPath = TestLocalization.putInto(earPath, WAR_NAME) + '/';
        warSpecification.setModulePath(warPath);

        warSpecification.setRootClassLoader(DT7_DT_war_Data.class.getClassLoader());

        return warSpecification;
    }
}
