parser grammar JCFG;

options {
    tokenVocab = JCFGLexer;
}

file: macro* (object | code)*  ;

macro: MacroStart Name Name* ;

value: object | (Name | Number | StringD | StringS) ;

setValue: Name (Colon | SEq) value ;

constructor: Constructor Name (Name value (',' Name value)*)? ;

objectName: Name ;

object: LB objectName? (code | func | setValue | constructor)* RB ;

code: Code ;
func: override modifier* retType funcName '(' (Name Name (',' Name Name)*)? ')' Code;

// function parts
modifier: Name ;
retType: Name ;
funcName: Name ;
override: Override ;