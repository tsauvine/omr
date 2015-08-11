/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package omr;

import java.io.FileInputStream;
import java.util.Properties;

/**
 *
 * @author noga
 */
public final class OMRProperties {
    
    public static Properties values;
    
    static{
     
        try{
            // Load properties
            values = new Properties() ;
            values.load(new FileInputStream("omr.properties"));
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    public static int getInt(String name, int defValue){
        String s = values.getProperty(name);
        if ( s != null ){ return Integer.parseInt(s); }
        return defValue;
    }
    
    public static double getDouble(String name, double defValue){
        String s = values.getProperty(name);
        if ( s != null ){ return Double.parseDouble(s); }
        return defValue;
    }
    
    public static String getString(String name, String defValue){
        String s = values.getProperty(name);
        return s != null ? s : defValue ;
    }
    
}
