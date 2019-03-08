package com.dgcheshang.cheji.Tools;

/**
 * Created by Administrator on 2019/2/25 0025.
 */

public class CarTypeUtil {

    public static String getCartypeNum(String cartype){
        String num="";
        if(cartype.equals("A1")){
            num="01";
        } else if (cartype.equals("A2")) {
            num="02";
        }else if (cartype.equals("A3")) {
            num="03";
        }else if (cartype.equals("B1")) {
            num="11";
        }else if (cartype.equals("B2")) {
            num="12";
        }else if (cartype.equals("C1")) {
            num="21";
        }else if (cartype.equals("C2")) {
            num="22";
        }else if (cartype.equals("C3")) {
            num="23";
        }else if (cartype.equals("C4")) {
            num="24";
        }else if (cartype.equals("C5")) {
            num="25";
        }else if (cartype.equals("D")) {
            num="31";
        }else if (cartype.equals("E")) {
            num="32";
        }else if (cartype.equals("F")) {
            num="33";
        }else if (cartype.equals("M")) {
            num="41";
        }else if (cartype.equals("N")) {
            num="42";
        }else if (cartype.equals("P")) {
            num="43";
        }
        return num;
    }
}
