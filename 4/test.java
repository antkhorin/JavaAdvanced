interface One<T extends java.util.Collection> {
    <Z extends java.util.Set> Comparable f(Z tt);
}

interface Two<T extends java.util.Collection> {
    <Z extends java.util.Set> Number f(Z ss);
}

interface A extends One<java.util.Set>, Two<java.util.Set> {

}

class AA implements A {
	public <Z extends java.util.Set> Integer f(Z ss) {
		return 0;
	}
//	public <Z extends java.util.Set> Integer f(java.util.Set ss) {
//		return 0;
//	}
}
