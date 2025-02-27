parser grammar JCFG;

options {
    tokenVocab = JCFGLexer;
}

file: macro* object*  ;

macro: MacroStart Name Name* ;

value: object | (Name | Number | StringD | StringS) ;

setValue: Name (Colon | SEq) value ;

constructor: Constructor Name (Name value (',' Name value)*)? ;
function: FuncStart code;

object: LB (function | setValue | constructor)* RB ;

code: Code ;
