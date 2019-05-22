# remove-bg
An unofficial android SDK for remove.bg

![](demo.gif)

## Installation

```gradle
implementation 'com.theapache64.remove-bg:remove-bg:0.0.1-alpha01'
```

### Usage

Initialize the SDK from your `Application` class. You can get your key from [here](https://www.remove.bg/profile#api-key)

```kotlin


import android.app.Application
import com.theapache64.removebg.RemoveBg

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        RemoveBg.init("YOUR-API-KEY")
    }
}
```

and you can use it like this

```kotlin
RemoveBg.from(imageFile, object : RemoveBg.RemoveBgCallback {

    override fun onProcessing() {
        // will be invoked once finished uploading the image
    }

    override fun onUploadProgress(progress: Float) {
        // will be invoked on uploading 
    }

    override fun onError(errors: List<ErrorResponse.Error>) {
        // will be invoked if there's any error occurred
    }

    override fun onSuccess(bitmap: Bitmap) {
        // will be invoked when background removed 
    }

})
```

### Author

theapache64

