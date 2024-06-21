package systems.symbol.controller.ux;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.controller.responses.SimpleResponse;
import systems.symbol.string.Validate;
import systems.symbol.util.Stopwatch;

import java.io.File;

@Path("/ux/files")
@Tag(name = "api.ux.files.name", description = "api.ux.files.description")
public class FilesAPI extends GuardedAPI {

    @GET
    @Operation(
            summary = "api.ux.files.get.summary",
            description = "api.ux.files.get.description"
    )
    @Path("{fingerprint:.*}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response download(@PathParam("fingerprint") String fingerprint,   @HeaderParam("Authorization") String auth) {
        Stopwatch stopwatch = new Stopwatch();
        log.info("ux.files: {} ", fingerprint);
        DecodedJWT jwt;
        try {
            jwt = authenticate(auth);
        } catch (OopsException e) {
            return new OopsResponse(e.getMessage(), e.getStatus()).asJSON();
        }

        File home = new File(platform.getWorkspace().getHome(), "vfs");
        File file = new File(home, fingerprint + "/file");

        log.info("ipfs.download: {} @ {} == {}", fingerprint, file, file.exists() );
        if (!file.exists()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new SimpleResponse("error", "File not found").asJSON())
                    .build();
        }


        return Response.ok(file)
                .header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"")
                .build();
    }

//    @POST
//    @Consumes(MediaType.MULTIPART_FORM_DATA)
//    public void uploadMultiple(@("file") FormBodyPart body){
//        for(BodyPart part : body.getBodyParts()){
//            InputStream is = part.getEntityAs(InputStream.class);
//            ContentDisposition meta = part.getContentDisposition();
//            doUpload(is, meta);
//        }
//    }
//
//    @POST
//    @Consumes(MediaType.MULTIPART_FORM_DATA)
//    public Response upload(@FormDataParam("file") List<InputStream> uploadedInputStreams,
//                           @FormDataParam("file") List<FormDataContentDisposition> fileDetails) {
//        File home = new File(platform.getWorkspace().getHome(), "vfs");
//        StringBuilder fingerprints = new StringBuilder();
//
//        List<FormDataBodyPart> parts = formParams.getFields("file");
//        for (FormDataBodyPart part : parts) {
//            FormDataContentDisposition file = part.getFormDataContentDisposition();
//        }
//
//        for (int i = 0; i < uploadedInputStreams.size(); i++) {
//            InputStream uploadedInputStream = uploadedInputStreams.get(i);
//            FormDataContentDisposition fileDetail = fileDetails.get(i);
//
//            try {
//                // Create a temporary file
//                File tempFile = File.createTempFile("upload", ".blob");
//                try (FileOutputStream out = new FileOutputStream(tempFile)) {
//                    IOUtils.copy(uploadedInputStream, out);
//                }
//
//                // Generate the fingerprint
//                String fingerprint = Fingerprint.identify(tempFile);
//                if (Validate.isMissing(fingerprint)) {
//                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
//                            .entity(new SimpleResponse("error", "File upload failed").asJSON())
//                            .build();
//                }
//
//                // Create the target directory
//                File to = new File(home, fingerprint);
//                if (!to.exists() && !to.mkdirs()) {
//                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
//                            .entity(new SimpleResponse("error", "Directory creation failed").asJSON())
//                            .build();
//                }
//
//                // Move the temporary file to the target directory
//                File targetFile = new File(to, fileDetail.getFileName());
//                Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
//
//                if (fingerprints.length() > 0) {
//                    fingerprints.append(", ");
//                }
//                fingerprints.append(fingerprint);
//
//            } catch (IOException e) {
//                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
//                        .entity(new SimpleResponse("error", "File upload failed").asJSON())
//                        .build();
//            }
//        }
//
//        return new SimpleResponse("ipfs", fingerprints.toString()).asJSON();
//    }
}
