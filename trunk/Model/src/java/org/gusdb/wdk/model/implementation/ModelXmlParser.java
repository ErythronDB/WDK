package org.gusdb.gus.wdk.model.implementation;

import org.gusdb.gus.wdk.model.Column;
import org.gusdb.gus.wdk.model.Record;
import org.gusdb.gus.wdk.model.RecordSet;
import org.gusdb.gus.wdk.model.Reference;
import org.gusdb.gus.wdk.model.QuerySet;
import org.gusdb.gus.wdk.model.Param;
import org.gusdb.gus.wdk.model.TextField;
import org.gusdb.gus.wdk.model.WdkModel;
import org.gusdb.gus.wdk.model.QueryNameList;
import org.gusdb.gus.wdk.model.Query;
import org.gusdb.gus.wdk.model.QueryName;
import org.gusdb.gus.wdk.model.RecordListSet;
import org.gusdb.gus.wdk.model.RecordList;


import java.io.File;

import org.apache.commons.digester.Digester;

public class ModelXmlParser {

    public static WdkModel parseXmlFile(File modelXmlFile) throws java.io.IOException, org.xml.sax.SAXException, Exception {
	Digester digester = configureDigester();
	WdkModel model = 
	    (WdkModel)digester.parse(modelXmlFile);
	model.resolveReferences();
	return model;
    }

    static Digester configureDigester() {

	Digester digester = new Digester();
	digester.setValidating(true);
	digester.setNamespaceAware(true);
	digester.setSchemaLanguage("http://www.w3.org/2001/XMLSchema");
	digester.setSchema("wdkModelSchema.xsd");
	digester.setErrorHandler(new ParserErrorHandler());

	//Root -- WDK Model

	digester.addObjectCreate( "wdkModel", WdkModel.class );
	digester.addSetProperties( "wdkModel");


	
	//RecordSet

	/**/ digester.addObjectCreate( "wdkModel/recordSet", RecordSet.class );

	/**/ digester.addSetProperties( "wdkModel/recordSet");
	
	/*  */ digester.addObjectCreate( "wdkModel/recordSet/record", Record.class );

	/*  */ digester.addSetProperties( "wdkModel/recordSet/record");

	/*    */ digester.addObjectCreate( "wdkModel/recordSet/record/fieldsQuery", Reference.class );

	/*    */ digester.addSetProperties( "wdkModel/recordSet/record/fieldsQuery");

	/*    */ digester.addSetNext( "wdkModel/recordSet/record/fieldsQuery", "addFieldsQueryRef" );

	/*    */ digester.addObjectCreate( "wdkModel/recordSet/record/tableQuery", Reference.class );

	/*    */ digester.addSetProperties( "wdkModel/recordSet/record/tableQuery");

	/*    */ digester.addSetNext( "wdkModel/recordSet/record/tableQuery", "addTableQueryRef" );

	/*    */ digester.addObjectCreate( "wdkModel/recordSet/record/textField", TextField.class );

	/*    */ digester.addSetProperties( "wdkModel/recordSet/record/textField");

	/*      */ digester.addBeanPropertySetter( "wdkModel/recordSet/record/textField/text");

	/*    */ digester.addSetNext( "wdkModel/recordSet/record/textField", "addTextField" );

	/*  */ digester.addSetNext( "wdkModel/recordSet/record", "addRecord" );

	/**/ digester.addSetNext( "wdkModel/recordSet", "addRecordSet" );


	//QuerySet


	/**/ digester.addObjectCreate( "wdkModel/querySet", QuerySet.class );

	/**/ digester.addSetProperties( "wdkModel/querySet");
	
	/*  */ digester.addObjectCreate( "wdkModel/querySet/query", "xsi:type", Query.class );

	/*  */ digester.addSetProperties( "wdkModel/querySet/query");

	/*  */ digester.addBeanPropertySetter( "wdkModel/querySet/query/sql");

	/*    */ digester.addObjectCreate( "wdkModel/querySet/query/param", "xsi:type", Param.class );
	/*    */ digester.addSetProperties( "wdkModel/querySet/query/param");

	/*      */ digester.addObjectCreate( "wdkModel/querySet/query/param/query", "xsi:type", Query.class );

	/*        */ digester.addBeanPropertySetter( "wdkModel/querySet/query/param/query/sql");

	/*      */ digester.addSetNext( "wdkModel/querySet/query/param/query", "setSqlQuery");

	/*    */ digester.addSetNext( "wdkModel/querySet/query/param", "addParam" );
	
	
	/*    */ digester.addObjectCreate( "wdkModel/querySet/query/column", "xsi:type", Column.class );

	/*    */ digester.addSetProperties( "wdkModel/querySet/query/column");

	/*    */ digester.addSetNext( "wdkModel/querySet/query/column", "addColumn" );

	/*  */ digester.addSetNext( "wdkModel/querySet/query", "addQuery" );

	/**/ digester.addSetNext( "wdkModel/querySet", "addQuerySet" );


	//QueryNameList
	
	/**/ digester.addObjectCreate("wdkModel/queryNameList", QueryNameList.class);

	/**/ digester.addSetProperties("wdkModel/queryNameList");
	
	/*  */ digester.addObjectCreate("wdkModel/queryNameList/fullQueryName", QueryName.class);

	/*  */ digester.addSetProperties("wdkModel/queryNameList/fullQueryName");

	/*  */ digester.addSetNext("wdkModel/queryNameList/fullQueryName", "addQueryName");

	/**/ digester.addSetNext("wdkModel/queryNameList", "addQueryNameList");

	//RecordListSet
	
	/**/ digester.addObjectCreate("wdkModel/recordListSet", RecordListSet.class);

	/**/ digester.addSetProperties("wdkModel/recordListSet");

	/*  */ digester.addObjectCreate("wdkModel/recordListSet/recordList", RecordList.class);

	/*  */ digester.addSetProperties("wdkModel/recordListSet/recordList");

	/*  */ digester.addSetNext("wdkModel/recordListSet/recordList", "addRecordList");

        /**/ digester.addSetNext("wdkModel/recordListSet", "addRecordListSet");

	return digester;
    }
    
    public static void main( String[] args ) {
	try {
	    File querySetXmlFile = new File(args[0]);
	    WdkModel wdkModel = parseXmlFile(querySetXmlFile);
	    
	    System.out.println( wdkModel.toString() );
	    
	} catch( Exception exc ) {
	    exc.printStackTrace();
	}
    }
}


