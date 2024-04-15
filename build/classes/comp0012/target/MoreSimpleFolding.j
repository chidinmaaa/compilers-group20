.source Type1.j
.class public comp0012/target/MoreSimpleFolding
.super java/lang/Object

.method public <init>()V
	aload_0
	invokenonvirtual java/lang/Object/<init>()V
	return
.end method

.method public simpleSubDouble()V
	.limit stack 5

	getstatic java/lang/System/out Ljava/io/PrintStream;
	ldc 7.5
	ldc 4.5
    dsub
    invokevirtual java/io/PrintStream/println(I)V
	return
.end method

.method public simpleMulFloat()V
	.limit stack 5

	getstatic java/lang/System/out Ljava/io/PrintStream;
	ldc 850.321
	ldc 2.5
    fmul
    invokevirtual java/io/PrintStream/println(I)V
	return
.end method

.method public simpleDivLong()V
	.limit stack 5

	getstatic java/lang/System/out Ljava/io/PrintStream;
	ldc 12345678910
	ldc 12345678910
    ldiv
    invokevirtual java/io/PrintStream/println(I)V
	return
.end method