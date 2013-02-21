package org.braindroppings.forkingjava;

import java.io.Serializable;
import java.util.concurrent.Callable;

public interface RemoteOperation<T> extends Callable<T>, Serializable {

}
