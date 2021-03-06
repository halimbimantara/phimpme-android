package org.fossasia.phimpme.accounts

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import io.realm.RealmQuery
import org.fossasia.phimpme.R
import org.fossasia.phimpme.data.local.AccountDatabase
import org.fossasia.phimpme.utilities.ActivitySwitchHelper.context
import org.fossasia.phimpme.utilities.ActivitySwitchHelper.getContext

/**
 * Created by pa1pal on 5/6/17.
 */
class AccountAdapter : RecyclerView.Adapter<AccountAdapter.ViewHolder>() {

    private var accountDetails: RealmQuery<AccountDatabase>? = null
    val accountsName = arrayOf("Twitter", "Facebook", "Instagram","Drupal")
    //val accountsLogo = arrayOf(R.drawable.ic_twitter, R.drawable.ic_facebook,R.drawable.ic_instagram)

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent!!.getContext())
                .inflate(R.layout.accounts_item_view, null, false)
        view.layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        /*if (accountDetails?.size || accountDetails?.get(position)?.name != null) {
            holder!!.accountName!!.text = accountDetails!![position].name
            holder!!.signInSignOutSwitch?.isChecked = true
        } else {
            holder!!.accountName!!.text = accountsName[position]
        }
*/
        if (accountDetails?.equalTo("name", accountsName[position])?.findAll()?.size ?: 0 > 0){
            holder!!.accountName!!.text =  accountDetails?.equalTo("name", accountsName[position])
                    ?.findAll()?.get(0)?.username

            holder.signInSignOutSwitch!!.isChecked = true

        } else {
            holder!!.accountName!!.text = accountsName[position]
        }

        /**
         * Selecting the image resource id on the basis of name of the account.
         */
        var id = getContext().resources.getIdentifier(context.getString(R.string.ic_) +
                (accountsName[position].toLowerCase()) + "_black"
                , context.getString(R.string.drawable)
                , getContext().packageName)

        holder!!.accountAvatar!!.setImageResource(id)
    }

    override fun getItemCount(): Int {
        //TODO: added a temporary value. Exact value will be depends how many accounts are signed in.
        //return accountDetails?.size ?: 0
        return accountsName?.size ?: 0
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        internal var accountAvatar: ImageView? = null
        internal var accountName: TextView? = null
        internal var accountLogoIndicator: ImageView? = null
        internal var signInSignOutSwitch: Switch? = null

        init {
            accountAvatar = v.findViewById(R.id.account_avatar) as ImageView
            accountLogoIndicator = v.findViewById(R.id.account_logo_indicator) as ImageView
            accountName = v.findViewById(R.id.account_username) as TextView
            signInSignOutSwitch = v.findViewById(R.id.sign_in_sign_out_switch) as Switch
        }
    }

    fun setResults(accountDetails: RealmQuery<AccountDatabase>) {
        this.accountDetails = accountDetails
        notifyDataSetChanged()
    }

}