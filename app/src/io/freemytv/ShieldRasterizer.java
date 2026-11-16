package io.freemytv;

import android.graphics.Bitmap;

/** Alpha-aware Lanczos-3 reduction, performed once per physical display size off the UI thread. */
final class ShieldRasterizer {
    private static final class Kernel {
        final int first;final float[] weights;
        Kernel(int first,float[] weights){this.first=first;this.weights=weights;}
    }
    static Bitmap reduce(Bitmap source,int longest){
        int sw=source.getWidth(),sh=source.getHeight();float ratio=longest/(float)Math.max(sw,sh);
        int dw=Math.max(1,Math.round(sw*ratio)),dh=Math.max(1,Math.round(sh*ratio));
        if(dw==sw&&dh==sh)return source;
        int[] input=new int[sw*sh];source.getPixels(input,0,sw,0,0,sw,sh);
        Kernel[] xs=kernels(sw,dw),ys=kernels(sh,dh);
        float[] intermediate=new float[sh*dw*4];
        for(int y=0;y<sh;y++)for(int x=0;x<dw;x++){
            Kernel k=xs[x];float a=0,r=0,g=0,b=0;
            for(int i=0;i<k.weights.length;i++){
                int argb=input[y*sw+k.first+i];float weight=k.weights[i],alpha=argb>>>24,coverage=alpha/255f;
                a+=alpha*weight;r+=((argb>>16)&255)*coverage*weight;g+=((argb>>8)&255)*coverage*weight;b+=(argb&255)*coverage*weight;
            }
            int offset=(y*dw+x)*4;intermediate[offset]=a;intermediate[offset+1]=r;intermediate[offset+2]=g;intermediate[offset+3]=b;
        }
        int[] output=new int[dw*dh];
        for(int y=0;y<dh;y++)for(int x=0;x<dw;x++){
            Kernel k=ys[y];float a=0,r=0,g=0,b=0;
            for(int i=0;i<k.weights.length;i++){int offset=((k.first+i)*dw+x)*4;float weight=k.weights[i];a+=intermediate[offset]*weight;r+=intermediate[offset+1]*weight;g+=intermediate[offset+2]*weight;b+=intermediate[offset+3]*weight;}
            a=Math.max(0,Math.min(255,a));if(a<.5f){output[y*dw+x]=0;continue;}
            output[y*dw+x]=(Math.round(a)<<24)|(channel(r*255/a)<<16)|(channel(g*255/a)<<8)|channel(b*255/a);
        }
        return Bitmap.createBitmap(output,dw,dh,Bitmap.Config.ARGB_8888);
    }
    private static int channel(float value){return Math.max(0,Math.min(255,Math.round(value)));}
    private static Kernel[] kernels(int source,int target){
        Kernel[] out=new Kernel[target];double scale=source/(double)target,filter=Math.max(1,scale),radius=3*filter;
        for(int p=0;p<target;p++){
            double center=(p+.5)*scale-.5;int first=Math.max(0,(int)Math.ceil(center-radius)),last=Math.min(source-1,(int)Math.floor(center+radius));
            float[] weights=new float[last-first+1];double sum=0;
            for(int i=0;i<weights.length;i++){double distance=(first+i-center)/filter;double weight=lanczos(distance);weights[i]=(float)weight;sum+=weight;}
            for(int i=0;i<weights.length;i++)weights[i]/=(float)sum;out[p]=new Kernel(first,weights);
        }
        return out;
    }
    private static double lanczos(double x){x=Math.abs(x);if(x<1e-8)return 1;if(x>=3)return 0;double pi=Math.PI*x;return Math.sin(pi)*Math.sin(pi/3)/(pi*pi/3);}
}
