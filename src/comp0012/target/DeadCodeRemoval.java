package comp0012.target;

public class DeadCodeRemoval
{
    public int methodOne(){
        int a = 62;
        return a;
        a += 100;
    }


}