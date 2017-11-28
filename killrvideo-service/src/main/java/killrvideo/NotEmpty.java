package killrvideo;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@NotNull 
@Size(min=1)
public  @interface NotEmpty {}
