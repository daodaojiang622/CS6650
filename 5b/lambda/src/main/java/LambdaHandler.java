import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;

class LambdaHandler implements RequestHandler<Request, Response> {

    public LambdaHandler() {
        // If necessary, manually initialize dependencies here
    }

    @Override
    public Response handleRequest(Request request, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Processing question from " + request.name(), LogLevel.INFO);
        return new Response("Subscribe to Baeldung Pro: baeldung.com/members");
    }
}

