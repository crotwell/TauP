#include "taupnative.h"

/** C interface to the TauP package.
  * @version 0.94 Mon May 11 15:01:34 GMT 1998
  * @author H. Philip Crotwell
  *
  * Fortran interface by G. Helffrich/U. Bristol 26 Feb. 2007
  *
  * Fortran declarations:
  *   integer function taupinit(model)
  *   - return value zero means failure, otherwise model index (mix)
  *   integer function taupdestroy(model)
  *   - return value zero means OK, otherwise failure
  *   integer function taupsetdepth(mix, depth)
  *   - return value zero for OK
  *   integer function taupcleararrivals(mix)
  *   - return value zero for OK
  *   integer function taupappendarrivals(mix, list)
  *   - list is comma-separated phase name list character string; 
  *     return value zero for OK
  *   integer function taupcalculate(mix)
  *   - return value zero for OK
  *   integer function taupnumarrivals(mix)
  *   - returns number of arrivals after taupcalculate; -1 if error, >=0
  *     if OK
  *   character*(*) function taupgetarrivalname(mix, i)
  *   - returns name in model mix for arrival i (first i is 1)
  *   character*(*) function taupgetarrivalpuristname(mix, i)
  *   - returns purist name in model mix for arrival i (first i is 1)
  *   real function taupgetarrivaltime(mix, i)
  *   - returns arrival time (s) in model mix for arrival i (first i is 1)
  *   real function taupgetarrivalrayparam(mix, i)
  *   - returns arrival p (s/deg) in model mix for arrival i (first i is 1)
  *   real function taupgetarrivalsourcedepth(mix, i)
  *   - returns source depth for arrival (km) in model mix for arrival i
  *     (first i is 1)
  *   real function taupgetarrivaldist(mix, i)
  *   - returns distance for arrival (deg) in model mix for arrival i
  *     (first i is 1)
  */


struct tent {
   int num, used;
   TauPStruct ptr[];
};

static struct tent *ents = NULL;

int taupinit_(char *model, int mlen) {

   int i, ret;
   char *ptr;

   /* Make sure space exists in table for new entry */

   if (!ents || ents->used >= ents->num) {
      /* Get or expand table */
      struct tent *pent;
      int nent = 5;

      if (ents) nent = ents->used * 2;
      pent = malloc(sizeof(struct tent) + nent*sizeof(TauPStruct));
      if (pent == NULL) return 0;
      pent->num = nent;
      if (ents) {
	 pent->used = ents->used;
	 for(i=0; i<ents->num; i++) pent->ptr[i] = ents->ptr[i];
	 free(ents);
      } else {
         pent->used = 0;
      }
      ents = pent;
   }

   /* Check model name is zero-terminated or blank terminated; if not, copy
      and zero-terminate. */
   for (i=0; i<mlen; i++)
      if (model[i] == '\0' || model[i] == ' ') break;
   if (model[i]) {
      char *mptr = malloc(mlen+1);
      if (mptr == NULL) return 0;
      strncpy(mptr, model, mlen);
      mptr[model[i]==' '?i:mlen] = '\0';
      ptr = mptr;
   } else
      ptr = model;

   /* Initialize model, free pointer copy if needed, return*/
   ret = TauPInit(ents->ptr + ents->used, ptr);
   if (ptr != model) free(ptr);

   if (!ret) ret = 1 + ++ents->used;

   return ret;

}

/** Initializes the Java Virtual Machine as well as initializes the 
 *  travel time tool with the given modelname. A properly filled in TauPStruct
 *  allocated before the call is returned as the first arguement. 
 *  If an error occurs, then a nonzero error code is returned. */
int TauPInit(TauPStruct *taupptr, char *modelName) {

	JavaVMInitArgs vm_args;
	JavaVMOption options[2];
	jint res;
	jmethodID constrID;
	jstring jstr;
	jobject tempTool;
	jclass tempClass;
	jobjectArray args;
	char *envclasspath;
	char *classpath;
   char *classpath_prefix = "-Djava.class.path=";
	char *envtauppath;
	jmethodID tempMethodID;
	jobject tempobject;
	jstring jstrvalue;
	jobject props;
	jthrowable exception;
   int classpathlength;

   envclasspath = (char*)getenv("CLASSPATH");
	if (envclasspath == NULL) {
      envclasspath = ".";
	}

	/* IMPORTANT: specify vm_args version # if you use JDK1.1.2 and beyond */
	vm_args.version = JNI_VERSION_1_2;

	/* added environment variable CLASSPATH to vm_args.classpath */
#if 0
	JNI_GetDefaultJavaVMInitArgs(&vm_args);
	if ((envclasspath = getenv("CLASSPATH")) != NULL) {
		int classpathlength = strlen(envclasspath) + strlen(vm_args.classpath) +1;
		char * newclasspath = (char*)malloc(classpathlength * sizeof(char));
		strcpy(newclasspath, envclasspath);
		strcat(newclasspath, vm_args.classpath);
		vm_args.classpath = newclasspath;
	}
#else
	vm_args.nOptions = 0;
	vm_args.options = options;
#if 1
#define CPNAME "-Djava.class.path="
	if ((envclasspath = getenv("CLASSPATH")) != NULL) {
        	int classpathlength = strlen(envclasspath) + strlen(CPNAME) + 1;
		char * newclasspath = (char*)malloc(classpathlength * sizeof(char));
		strcpy(newclasspath, CPNAME);
		strcat(newclasspath, envclasspath);
		options[vm_args.nOptions++].optionString = newclasspath;
	}
#endif
#endif

		/* create the Java Virtual Machine. */
	res = JNI_CreateJavaVM(&(taupptr->jvm), (void *)&(taupptr->env), &vm_args);
	if (res < 0) {
		fprintf(stderr, "Can't create Java VM: %d\n", res);
		return(1);
	}

		/* get the class of the travel time tool. */
	tempClass = (*taupptr->env)->FindClass(taupptr->env, "edu/sc/seis/TauP/TauP_Time");
	if (tempClass == 0) {
		fprintf(stderr, "Can't find edu.sc.seis.TauP.TauP_Time class\n");
		return(1);
	}
	taupptr->toolClass = (*taupptr->env)->NewGlobalRef(taupptr->env, tempClass);
 
		/* if the TAUPPATH env variable is defined, then add it to the properties.
		 */
	if ((envtauppath = (char*)getenv("TAUPPATH")) != NULL) {
		jstr = (*taupptr->env)->NewStringUTF(taupptr->env,"taup.model.path");
		jstrvalue = (*taupptr->env)->NewStringUTF(taupptr->env, envtauppath);
		tempClass = (*taupptr->env)->FindClass(taupptr->env, "java/lang/System");
		tempMethodID = (*taupptr->env)->GetStaticMethodID(taupptr->env, 
			tempClass, "getProperties", "()Ljava/util/Properties;");
		props = (*taupptr->env)->CallStaticObjectMethod(taupptr->env, 
			tempClass,  tempMethodID);
		tempClass = (*taupptr->env)->GetObjectClass(taupptr->env, props);
		tempMethodID = (*taupptr->env)->GetMethodID(taupptr->env, tempClass, 
			"put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
		tempobject = (*taupptr->env)->CallObjectMethod(taupptr->env, 
			props, tempMethodID, jstr, jstrvalue);
	}

		/* get the constructor for the tool */
	constrID = (*taupptr->env)->GetMethodID(taupptr->env, taupptr->toolClass, 
		"<init>", "(Ljava/lang/String;)V");
	if (constrID == 0) {
		fprintf(stderr, "Can't find constructor for tool\n");
		return(1);
	}
		/* convert the C string for the model name to a Java string */
	jstr = (*taupptr->env)->NewStringUTF(taupptr->env, modelName);
	if (jstr == 0) {
		fprintf(stderr, "Out of memory\n");
		return(1);
	}
		/* create the tool, using the constructor and modelname */
	tempTool = (*taupptr->env)->NewObject(taupptr->env, taupptr->toolClass, constrID, jstr);
	if ((exception = (*taupptr->env)->ExceptionOccurred(taupptr->env)) != NULL) {
		fprintf(stderr,"Caught Exception in TauPInit, Failed to create tool\n");
		(*taupptr->env)->ExceptionDescribe(taupptr->env);
		return 1;
	}
	if (tempTool == NULL) {
		fprintf(stderr, "Failed to create tool\n");
		return(1);
	}
	taupptr->tool = (*taupptr->env)->NewGlobalRef(taupptr->env, tempTool);

		/* find the method IDs for common methods and store them in TauPStruct */
	taupptr->depthCorID = (*taupptr->env)->GetMethodID(taupptr->env, taupptr->toolClass, "depthCorrect", "(D)V");
	if (taupptr->depthCorID == NULL) {
		fprintf(stderr, "taupptr->depthCorID is NULL\n");
		return(1);
	}

	taupptr->parsePhaseID = (*taupptr->env)->GetMethodID(taupptr->env, taupptr->toolClass, "parsePhaseList", "(Ljava/lang/String;)V");
	if (taupptr->parsePhaseID == NULL) {
		fprintf(stderr, "taupptr->parsePhaseID is NULL\n");
		return(1);
	}

	taupptr->clearPhasesID = (*taupptr->env)->GetMethodID(taupptr->env, taupptr->toolClass, "clearPhaseNames", "()V");
	if (taupptr->clearPhasesID == NULL) {
		fprintf(stderr, "taupptr->clearPhasesID is NULL\n");
		return(1);
	}

	taupptr->calcID = (*taupptr->env)->GetMethodID(taupptr->env, taupptr->toolClass, "calculate", "(D)V");
	if (taupptr->calcID == NULL) {
		fprintf(stderr, "taupptr->calcID is NULL\n");
		return(1);
	}

	tempClass = (*taupptr->env)->FindClass(taupptr->env, "edu/sc/seis/TauP/Arrival");
	taupptr->arrivalClass = (*taupptr->env)->NewGlobalRef(taupptr->env, tempClass);

	taupptr->getNumArrivalsID = (*taupptr->env)->GetMethodID(taupptr->env, taupptr->toolClass, "getNumArrivals", "()I");
	if (taupptr->getNumArrivalsID == NULL) {
		fprintf(stderr, "taupptr->getNumArrivalsID is NULL\n");
		return 1;
	}

	taupptr->getArrivalID = (*taupptr->env)->GetMethodID(taupptr->env, taupptr->toolClass, "getArrival", "(I)Ledu/sc/seis/TauP/Arrival;");
	if (taupptr->getArrivalID == NULL) {
		fprintf(stderr, "taupptr->getArrivalID is NULL\n");
		return 1;
	}

	taupptr->getArrivalName = (*taupptr->env)->GetMethodID(taupptr->env, taupptr->arrivalClass, "getName", "()Ljava/lang/String;");
	if (taupptr->getArrivalName == NULL) {
		fprintf(stderr, "taupptr->getArrivalName is NULL\n");
		return 1;
	}

	taupptr->getArrivalPuristName = (*taupptr->env)->GetMethodID(taupptr->env, taupptr->arrivalClass, "getPuristName", "()Ljava/lang/String;");
	if (taupptr->getArrivalPuristName == NULL) {
		fprintf(stderr, "taupptr->getArrivalPuristName is NULL\n");
		return 1;
	}

	taupptr->getArrivalTime = (*taupptr->env)->GetMethodID(taupptr->env, taupptr->arrivalClass, "getTime", "()D");
	if (taupptr->getArrivalTime == NULL) {
		fprintf(stderr, "taupptr->getArrivalTime is NULL\n");
		return 1;
	}

	taupptr->getArrivalDist = (*taupptr->env)->GetMethodID(taupptr->env, taupptr->arrivalClass, "getDist", "()D");
	if (taupptr->getArrivalDist == NULL) {
		fprintf(stderr, "taupptr->getArrivalDist is NULL\n");
		return 1;
	}

	taupptr->getArrivalRayParam = (*taupptr->env)->GetMethodID(taupptr->env, taupptr->arrivalClass, "getRayParam", "()D");
	if (taupptr->getArrivalRayParam == NULL) {
		fprintf(stderr, "taupptr->getArrivalRayParam is NULL\n");
		return 1;
	}

	taupptr->getArrivalSourceDepth = (*taupptr->env)->GetMethodID(taupptr->env, taupptr->arrivalClass, "getSourceDepth", "()D");
	if (taupptr->getArrivalSourceDepth == NULL) {
		fprintf(stderr, "taupptr->getArrivalSourceDepth is NULL\n");
		return 1;
	}

	if ((exception = (*taupptr->env)->ExceptionOccurred(taupptr->env)) != NULL) {
		fprintf(stderr,"Caught Exception in TauPInit\n");
		(*taupptr->env)->ExceptionDescribe(taupptr->env);
		return 1;
	}

	return 0;
}

int taupsetdepth_(int *pid, float *depth) {

   int id = *pid - 1;

   if (ents == NULL || id < 1 || id > ents->used)
      return 1;

   return TauPSetDepth(ents->ptr[id-1], (double)*depth);
}

	/** set the source depth within the model, performing any needed
	 *  layer splitting, etc. */
int TauPSetDepth(TauPStruct taup, double depth) {
	jthrowable exception;
	jvalue args[1];

	if (taup.env == NULL) {
		fprintf(stderr, 
			"Error: TauPSetDepth taup.env is NULL, was TauPInit called?\n");
		return 1;
	}
	if (taup.tool == NULL) {
		fprintf(stderr, 
			"Error: TauPSetDepth taup.tool is NULL, was TauPInit called?\n");
		return 1;
	}

	args[0].d = (jdouble)depth;
	(*taup.env)->CallVoidMethodA(taup.env, taup.tool, taup.depthCorID, args);

	if ((exception = (*taup.env)->ExceptionOccurred(taup.env)) != NULL) {
		fprintf(stderr,"Caught Exception in TauPSetDepth\n");
		(*taup.env)->ExceptionDescribe(taup.env);
		return 1;
	}
	return 0;
}

int taupclearphases_(int *pid) {

   int id = *pid - 1;

   if (ents == NULL || id < 1 || id > ents->used)
      return 1;

   return TauPClearPhases(ents->ptr[id-1]);
}

/** clears the phases from the tool. Sould be followed by a call to
 *  TauPAppendPhases. returns 0 if successful, 1 if not. */
int TauPClearPhases(TauPStruct taup) {
   jthrowable exception;
 
   if (taup.env == NULL) {
      fprintf(stderr,
         "Error: TauPClearPhases taup.env is NULL, was TauPInit called?\n");
      return(1);
   }
   if (taup.tool == NULL) {
      fprintf(stderr,
         "Error: TauPClearPhases taup.tool is NULL, was TauPInit called?\n");
      return(1);
   }
 
   (*taup.env)->CallVoidMethod(taup.env, taup.tool, taup.clearPhasesID);
 
   if ((exception = (*taup.env)->ExceptionOccurred(taup.env)) != NULL) {
      fprintf(stderr,"Caught Exception in TauPCalculate\n");
      (*taup.env)->ExceptionDescribe(taup.env);
      return(1);
   }
 
   return(0);
}

int taupappendphases_(int *pid, char *str, int slen) {

   int i, ret, id = *pid - 1;
   char *ptr = NULL;

   if (ents == NULL || id < 1 || id > ents->used)
      return 1;

   /* Check model name is zero-terminated; if not, copy and zero-terminate. */
   for (i=0; i<slen; i++)
      if (str[i] == '\0') break;
   if (str[i]) {
      char *pptr = malloc(slen+1);
      if (pptr == NULL) return 1;
      strncpy(pptr, str, slen);
      str[slen] = '\0';
      ptr = pptr;
   } else
      ptr = str;

   /* Initialize model, free pointer copy if needed, return*/
   ret = TauPAppendPhases(ents->ptr[id-1], ptr);
   if (ptr != str) free(ptr);

   return ret;
}

	/** Appends the comma or space separated list of phase names that 
	 *  will be used for the calculations. */
int TauPAppendPhases(TauPStruct taup, char *phaseString) {
	jthrowable exception;
	jstring jstr;

	if (taup.env == NULL) {
		fprintf(stderr, 
			"Error: TauPAppendPhases taup.env is NULL, was TauPInit called?\n");
		return(1);
	}
	if (taup.tool == NULL) {
		fprintf(stderr, 
			"Error: TauPAppendPhases taup.tool is NULL, was TauPInit called?\n");
		return(1);
	}

	jstr = (*taup.env)->NewStringUTF(taup.env, phaseString);
	if (jstr == NULL) {
		fprintf(stderr, "TauPAppendPhases: Out of memory\n");
		exit(1);
	}

	(*taup.env)->CallVoidMethod(taup.env, taup.tool, taup.parsePhaseID, jstr);

	if ((exception = (*taup.env)->ExceptionOccurred(taup.env)) != NULL) {
		fprintf(stderr,"Caught Exception in TauPAppendPhases\n");
		(*taup.env)->ExceptionDescribe(taup.env);
		return(1);
	}

	return(0);
}

int taupcalculate_(int *pid, float *degrees) {

   int id = *pid - 1;

   if (ents == NULL || id < 1 || id > ents->used)
      return 1;

   return TauPCalculate(ents->ptr[id-1], (double)*degrees);
}

	/** Calculates travel times. Retrieval of the times is done by using
	 *  TauPGetNumArrivals, TauPGetArrivalTime and TauPGetArrivalName. */
int TauPCalculate(TauPStruct taup, double degrees) {
	jthrowable exception;
	jvalue args[1];

	if (taup.env == NULL) {
		fprintf(stderr, 
			"Error: TauPCalculate taup.env is NULL, was TauPInit called?\n");
		return(1);
	}
	if (taup.tool == NULL) {
		fprintf(stderr, 
			"Error: TauPCalculate taup.tool is NULL, was TauPInit called?\n");
		return(1);
	}

	args[0].d = (jdouble)degrees;
	(*taup.env)->CallVoidMethodA(taup.env, taup.tool, taup.calcID, args);

	if ((exception = (*taup.env)->ExceptionOccurred(taup.env)) != NULL) {
		fprintf(stderr,"Caught Exception in TauPCalculate\n");
		(*taup.env)->ExceptionDescribe(taup.env);
		return(1);
	}

	return(0);
}

int taupgetnumarrivals_(int *pid) {

   int id = *pid - 1;

   if (ents == NULL || id < 1 || id > ents->used)
      return -1;

   return TauPGetNumArrivals(ents->ptr[id-1]);
}

	/** gets number of arrivals from last TauPCalculate call. */
int TauPGetNumArrivals(TauPStruct taup) {
	jthrowable exception;
	int numArrivals;

	if (taup.env == NULL) {
		fprintf(stderr, 
			"Error: TauPCalculate taup.env is NULL, was TauPInit called?\n");
		return(-1);
	}
	if (taup.tool == NULL) {
		fprintf(stderr, 
			"Error: TauPCalculate taup.tool is NULL, was TauPInit called?\n");
		return(-1);
	}

	numArrivals = (*taup.env)->CallIntMethod(taup.env, taup.tool, taup.getNumArrivalsID);

	if ((exception = (*taup.env)->ExceptionOccurred(taup.env)) != NULL) {
		fprintf(stderr,"Caught Exception in TauPCalculate\n");
		(*taup.env)->ExceptionDescribe(taup.env);
		return(-1);
	}

	return numArrivals;
}

jobject TauPGetArrival(TauPStruct taup, int arrivalNum) {
   jthrowable exception;
   jobject arrival;
   jvalue args[1];
 
   if (taup.env == NULL) {
      fprintf(stderr,
         "Error: TauPCalculate taup.env is NULL, was TauPInit called?\n");
      return NULL;
   }
   if (taup.tool == NULL) {
      fprintf(stderr,
         "Error: TauPCalculate taup.tool is NULL, was TauPInit called?\n");
      return NULL;
   }
 
   args[0].i = (jint)arrivalNum;
   arrival = (*taup.env)->CallObjectMethodA(taup.env, taup.tool, taup.getArrivalID, args);
 
   if ((exception = (*taup.env)->ExceptionOccurred(taup.env)) != NULL) {
      fprintf(stderr,"Caught Exception in TauPCalculate\n");
      (*taup.env)->ExceptionDescribe(taup.env);
      return NULL;
   }
 
   return arrival;
}

void taupgetarrivalname_(char *__g77_result, int __g77_length,
   int *pid, int *pnum) {

   int i, id = *pid - 1;
   int num = *pnum;
   char *res;

   if (ents == NULL || id < 1 || id > ents->used)
      i = 0;  /* Bad id?  Just pad with blanks */
   else {
      res = TauPGetArrivalName(ents->ptr[id-1], num-1);
      if (res == NULL)
	 i = 0;
      else
	 for(i = 0; i<__g77_length && res[i]; i++) __g77_result[i] = res[i];
   }
   for(; i<__g77_length; i++) __g77_result[i] = ' '; /* blank pad */
}

	/** returns the name of the ith arrival. Returns NULL if there is an error.
	 */
char * TauPGetArrivalName(TauPStruct taup, int arrivalNum) {
   jthrowable exception;
   const char * tempArrivalName;
	char * arrivalName;
	jvalue args[1];
	jobject arrival, jarrivalName;
	jboolean isCopy;
	int len;
 
   if (taup.env == NULL) {
      fprintf(stderr,
         "Error: TauPGetArrivalName taup.env is NULL, was TauPInit called?\n");
      return NULL;
   }
   if (taup.tool == NULL) {
      fprintf(stderr,
         "Error: TauPGetArrivalName taup.tool is NULL, was TauPInit called?\n");
      return NULL;
   }
 
   arrival = TauPGetArrival(taup, arrivalNum);
	if (arrival == NULL) {
		fprintf(stderr, "TauPGetArrivalName: arrival is NULL\n");
		return NULL;
	}
	jarrivalName = (*taup.env)->CallObjectMethod(taup.env, arrival, taup.getArrivalName);
	tempArrivalName = (*taup.env)->GetStringUTFChars(taup.env, jarrivalName, &isCopy);
	len = strlen(tempArrivalName);
	arrivalName = (char*)malloc(len * sizeof(char));
	strcpy(arrivalName, tempArrivalName);
	if (isCopy == JNI_TRUE) {
		(*taup.env)->ReleaseStringUTFChars(taup.env, jarrivalName, tempArrivalName);
	}
 
   if ((exception = (*taup.env)->ExceptionOccurred(taup.env)) != NULL) {
      fprintf(stderr,"Caught Exception in TauPGetArrivalName\n");
      (*taup.env)->ExceptionDescribe(taup.env);
      return NULL;
   }
 
	return arrivalName;
}

void taupgetarrivalpuristname_(char *__g77_result, int __g77_length,
   int *pid, int *pnum) {

   int i, id = *pid - 1;
   int num = *pnum;
   char *res;

   if (ents == NULL || id < 1 || id > ents->used)
      i = 0;  /* Bad id?  Just pad with blanks */
   else {
      res = TauPGetArrivalPuristName(ents->ptr[id-1], num-1);
      if (res == NULL)
	 i = 0;
      else
	 for(i = 0; i<__g77_length && res[i]; i++) __g77_result[i] = res[i];
   }
   for(; i<__g77_length; i++) __g77_result[i] = ' '; /* blank pad */
}

	/** returns the purist name of the ith arrival. 
	  * Returns NULL if there is an error.
	  */
char * TauPGetArrivalPuristName(TauPStruct taup, int arrivalNum) {
   jthrowable exception;
   const char * tempArrivalName;
	char * arrivalName;
	jvalue args[1];
	jobject arrival, jarrivalName;
	jboolean isCopy;
	int len;
 
   if (taup.env == NULL) {
      fprintf(stderr,
         "Error: TauPGetArrivalPuristName taup.env is NULL, was TauPInit called?\n");
      return NULL;
   }
   if (taup.tool == NULL) {
      fprintf(stderr,
         "Error: TauPGetArrivalPuristName taup.tool is NULL, was TauPInit called?\n");
      return NULL;
   }
 
   arrival = TauPGetArrival(taup, arrivalNum);
	if (arrival == NULL) {
		fprintf(stderr, "TauPGetArrivalPuristName: arrival is NULL\n");
		return NULL;
	}
	jarrivalName = (*taup.env)->CallObjectMethod(taup.env, arrival, taup.getArrivalName);
	tempArrivalName = (*taup.env)->GetStringUTFChars(taup.env, jarrivalName, &isCopy);
	len = strlen(tempArrivalName);
	arrivalName = (char*)malloc(len * sizeof(char));
	strcpy(arrivalName, tempArrivalName);
	if (isCopy == JNI_TRUE) {
		(*taup.env)->ReleaseStringUTFChars(taup.env, jarrivalName, tempArrivalName);
	}
 
   if ((exception = (*taup.env)->ExceptionOccurred(taup.env)) != NULL) {
      fprintf(stderr,"Caught Exception in TauPGetArrivalPuristName\n");
      (*taup.env)->ExceptionDescribe(taup.env);
      return NULL;
   }
 
	return arrivalName;
}

float taupgetarrivaltime_(int *pid, int *n) {

   int id = *pid - 1;

   if (ents == NULL || id < 1 || id > ents->used)
      return -1;

   return (float)TauPGetArrivalTime(ents->ptr[id-1], *n - 1);
}

	/** returns time from ith arrival. Returns -1 if error. */
double TauPGetArrivalTime(TauPStruct taup, int arrivalNum) {
   jthrowable exception;
   int numArrivals;
	jobject arrival;
	double arrivalTime;
	jvalue args[1];
 
   if (taup.env == NULL) {
      fprintf(stderr,
         "Error: TauPGetArrivalTime taup.env is NULL, was TauPInit called?\n");
      return(-1);
   }
   if (taup.tool == NULL) {
      fprintf(stderr,
         "Error: TauPGetArrivalTime taup.tool is NULL, was TauPInit called?\n");
      return(-1);
   }
 
   arrival = TauPGetArrival(taup, arrivalNum);
	if (arrival == NULL) {
		fprintf(stderr, "TauPGetArrivalTime: arrival is NULL\n");
		return -1;
	}
	arrivalTime = (*taup.env)->CallDoubleMethod(taup.env, arrival, taup.getArrivalTime);
 
   if ((exception = (*taup.env)->ExceptionOccurred(taup.env)) != NULL) {
      fprintf(stderr,"Caught Exception in TauPGetArrivalTime\n");
      (*taup.env)->ExceptionDescribe(taup.env);
      return(-1);
   }
 
	return arrivalTime;
}

float taupgetarrivaldist_(int *pid, int *n) {

   int id = *pid - 1;

   if (ents == NULL || id < 1 || id > ents->used)
      return -1;

   return (float)TauPGetArrivalDist(ents->ptr[id-1], *n - 1)*180/3.141592;
}

	/** returns distance from ith arrival. Returns -1 if error. */
double TauPGetArrivalDist(TauPStruct taup, int arrivalNum) {
   jthrowable exception;
   int numArrivals;
	jobject arrival;
	double arrivalDist;
	jvalue args[1];
 
   if (taup.env == NULL) {
      fprintf(stderr,
         "Error: TauPGetArrivalDist taup.env is NULL, was TauPInit called?\n");
      return(-1);
   }
   if (taup.tool == NULL) {
      fprintf(stderr,
         "Error: TauPGetArrivalDist taup.tool is NULL, was TauPInit called?\n");
      return(-1);
   }
 
   arrival = TauPGetArrival(taup, arrivalNum);
	if (arrival == NULL) {
		fprintf(stderr, "TauPGetArrivalDist: arrival is NULL\n");
		return -1;
	}
	arrivalDist = (*taup.env)->CallDoubleMethod(taup.env, arrival, taup.getArrivalDist);
 
   if ((exception = (*taup.env)->ExceptionOccurred(taup.env)) != NULL) {
      fprintf(stderr,"Caught Exception in TauPGetArrivalDist\n");
      (*taup.env)->ExceptionDescribe(taup.env);
      return(-1);
   }
 
	return arrivalDist;
}

float taupgetarrivalrayparam_(int *pid, int *n) {

   int id = *pid - 1;

   if (ents == NULL || id < 1 || id > ents->used)
      return -1;

   return (float)TauPGetArrivalRayParam(ents->ptr[id-1], *n - 1)*3.141592/180;
}

	/** returns ray parameter from ith arrival. Returns -1 if error. */
double TauPGetArrivalRayParam(TauPStruct taup, int arrivalNum) {
   jthrowable exception;
   int numArrivals;
	jobject arrival;
	double arrivalRayParam;
	jvalue args[1];
 
   if (taup.env == NULL) {
      fprintf(stderr,
         "Error: TauPGetArrivalRayParam taup.env is NULL, was TauPInit called?\n");
      return(-1);
   }
   if (taup.tool == NULL) {
      fprintf(stderr,
         "Error: TauPGetArrivalRayParam taup.tool is NULL, was TauPInit called?\n");
      return(-1);
   }
 
   arrival = TauPGetArrival(taup, arrivalNum);
	if (arrival == NULL) {
		fprintf(stderr, "TauPGetArrivalRayParam: arrival is NULL\n");
		return -1;
	}
	arrivalRayParam = (*taup.env)->CallDoubleMethod(taup.env, arrival, taup.getArrivalRayParam);
 
   if ((exception = (*taup.env)->ExceptionOccurred(taup.env)) != NULL) {
      fprintf(stderr,"Caught Exception in TauPGetArrivalRayParam\n");
      (*taup.env)->ExceptionDescribe(taup.env);
      return(-1);
   }
 
	return arrivalRayParam;
}

float taupgetarrivalsourcedepth_(int *pid, int *n) {

   int id = *pid - 1;

   if (ents == NULL || id < 1 || id > ents->used)
      return -1;

   return (float)TauPGetArrivalSourceDepth(ents->ptr[id-1], *n - 1);
}

	/** returns source depth from ith arrival. Returns -1 if error. */
double TauPGetArrivalSourceDepth(TauPStruct taup, int arrivalNum) {
   jthrowable exception;
   int numArrivals;
	jobject arrival;
	double arrivalSourceDepth;
	jvalue args[1];
 
   if (taup.env == NULL) {
      fprintf(stderr,
         "Error: TauPGetArrivalSourceDepth taup.env is NULL, was TauPInit called?\n");
      return(-1);
   }
   if (taup.tool == NULL) {
      fprintf(stderr,
         "Error: TauPGetArrivalSourceDepth taup.tool is NULL, was TauPInit called?\n");
      return(-1);
   }
 
   arrival = TauPGetArrival(taup, arrivalNum);
	if (arrival == NULL) {
		fprintf(stderr, "TauPGetArrivalSourceDepth: arrival is NULL\n");
		return -1;
	}
	arrivalSourceDepth = (*taup.env)->CallDoubleMethod(taup.env, arrival, taup.getArrivalSourceDepth);
 
   if ((exception = (*taup.env)->ExceptionOccurred(taup.env)) != NULL) {
      fprintf(stderr,"Caught Exception in TauPGetArrivalSourceDepth\n");
      (*taup.env)->ExceptionDescribe(taup.env);
      return(-1);
   }
 
	return arrivalSourceDepth;
}

int taupdestroy_(int *pid) {

   int id = *pid - 1;

   if (ents == NULL || id < 1 || id > ents->used)
      return -1;

   return TauPDestroy(ents->ptr[id-1]);
}

	/** Destroys the Java Virtual Machine and frees the TauPStruct. */
int TauPDestroy(TauPStruct taup) {
	
	(*taup.env)->DeleteGlobalRef(taup.env, taup.tool);
	(*taup.env)->DeleteGlobalRef(taup.env, taup.toolClass);
	(*taup.env)->DeleteGlobalRef(taup.env, taup.arrivalClass);
	taup.tool = NULL;
	(*taup.jvm)->DestroyJavaVM(taup.jvm);

	return 0;
}

